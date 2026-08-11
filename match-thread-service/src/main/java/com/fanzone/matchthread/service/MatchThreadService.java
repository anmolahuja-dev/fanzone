package com.fanzone.matchthread.service;

import com.fanzone.common.enums.MatchPhase;
import com.fanzone.common.exceptions.BusinessRuleException;
import com.fanzone.common.exceptions.ForbiddenException;
import com.fanzone.common.exceptions.NotFoundException;
import com.fanzone.common.exceptions.ValidationException;
import com.fanzone.matchthread.dto.*;
import com.fanzone.matchthread.model.*;
import com.fanzone.matchthread.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MatchThreadService {

    private static final Logger log = LoggerFactory.getLogger(MatchThreadService.class);
    private static final Duration POST_MATCH_TIMEOUT = Duration.ofHours(48);
    private static final int MIN_RATINGS_FOR_MOTM = 10;

    private final MatchRepository matchRepository;
    private final MatchThreadCommentRepository commentRepository;
    private final PlayerRatingRepository playerRatingRepository;
    private final GoalReactionRepository goalReactionRepository;
    private final RateLimiterService rateLimiterService;

    public MatchThreadService(MatchRepository matchRepository,
                              MatchThreadCommentRepository commentRepository,
                              PlayerRatingRepository playerRatingRepository,
                              GoalReactionRepository goalReactionRepository,
                              RateLimiterService rateLimiterService) {
        this.matchRepository = matchRepository;
        this.commentRepository = commentRepository;
        this.playerRatingRepository = playerRatingRepository;
        this.goalReactionRepository = goalReactionRepository;
        this.rateLimiterService = rateLimiterService;
    }

    // ---- Match queries ----

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesForClub(UUID clubId) {
        List<MatchEntity> matches = matchRepository.findByClubAndStatuses(
                clubId, List.of(MatchPhase.SCHEDULED, MatchPhase.LIVE));
        return matches.stream().map(this::toMatchResponse).toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse getMatch(UUID matchId) {
        MatchEntity match = findMatchOrThrow(matchId);
        return toMatchResponse(match);
    }

    // ---- Comments ----

    @Transactional(readOnly = true)
    public List<MatchCommentResponse> getComments(UUID matchId) {
        findMatchOrThrow(matchId);
        return commentRepository.findByMatchIdOrderByCreatedAtDesc(matchId).stream()
                .map(c -> new MatchCommentResponse(c.getId(), c.getMatchId(), c.getUserId(), c.getContent(), c.getCreatedAt()))
                .toList();
    }

    public MatchCommentResponse submitComment(UUID matchId, UUID userId, String content) {
        MatchEntity match = findMatchOrThrow(matchId);

        // Enforce post-match timeout
        validateNotTimedOut(match);

        // Rate limit: max 20 comments per user per 60s
        if (!rateLimiterService.isAllowed(matchId, userId)) {
            throw new BusinessRuleException("RATE_LIMITED",
                    "Rate limit exceeded: maximum 20 comments per minute");
        }

        MatchThreadCommentEntity comment = new MatchThreadCommentEntity();
        comment.setMatchId(matchId);
        comment.setUserId(userId);
        comment.setContent(content);

        MatchThreadCommentEntity saved = commentRepository.save(comment);
        rateLimiterService.recordComment(matchId, userId);

        log.debug("User {} posted comment on match {}", userId, matchId);
        return new MatchCommentResponse(saved.getId(), saved.getMatchId(), saved.getUserId(),
                saved.getContent(), saved.getCreatedAt());
    }

    // ---- Player Ratings ----

    public void submitPlayerRating(UUID matchId, UUID userId, UUID playerId, int rating) {
        findMatchOrThrow(matchId);

        if (rating < 1 || rating > 10) {
            throw new ValidationException("RATING_OUT_OF_RANGE", "Rating must be between 1 and 10");
        }

        // Upsert: one rating per user per player per match
        PlayerRatingEntity existing = playerRatingRepository
                .findByMatchIdAndUserIdAndPlayerId(matchId, userId, playerId)
                .orElse(null);

        if (existing != null) {
            existing.setRating(rating);
            playerRatingRepository.save(existing);
        } else {
            PlayerRatingEntity entity = new PlayerRatingEntity();
            entity.setMatchId(matchId);
            entity.setUserId(userId);
            entity.setPlayerId(playerId);
            entity.setRating(rating);
            playerRatingRepository.save(entity);
        }

        log.debug("User {} rated player {} = {} for match {}", userId, playerId, rating, matchId);
    }

    // ---- Goal Reactions ----

    public void submitGoalReaction(UUID matchId, UUID userId, String reactionType) {
        findMatchOrThrow(matchId);

        GoalReactionEntity entity = new GoalReactionEntity();
        entity.setMatchId(matchId);
        entity.setUserId(userId);
        entity.setReactionType(reactionType);
        goalReactionRepository.save(entity);

        log.debug("User {} reacted to goal in match {} with {}", userId, matchId, reactionType);
    }

    // ---- MOTM Calculation ----

    @Transactional(readOnly = true)
    public List<MotmResponse> calculateMotm(UUID matchId) {
        findMatchOrThrow(matchId);

        List<Object[]> topRated = playerRatingRepository.findTopRatedPlayers(matchId, MIN_RATINGS_FOR_MOTM);

        if (topRated.isEmpty()) {
            return List.of();
        }

        // Return top-3 (or fewer if less data)
        int limit = Math.min(3, topRated.size());
        return topRated.subList(0, limit).stream()
                .map(row -> new MotmResponse(
                        (UUID) row[0],
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).longValue(),
                        topRated.indexOf(row) + 1
                ))
                .toList();
    }

    // ---- Phase management ----

    public void updatePhase(UUID matchId, MatchPhase newPhase) {
        MatchEntity match = findMatchOrThrow(matchId);
        MatchPhase currentPhase = match.getStatus();

        if (!isValidTransition(currentPhase, newPhase)) {
            throw new BusinessRuleException("INVALID_PHASE_TRANSITION",
                    "Cannot transition from " + currentPhase + " to " + newPhase);
        }

        match.setStatus(newPhase);
        if (newPhase == MatchPhase.FINISHED) {
            match.setFinishedAt(Instant.now());
        }
        matchRepository.save(match);

        log.info("Match {} phase changed: {} -> {}", matchId, currentPhase, newPhase);
    }

    // ---- Pure logic exposed for property testing ----

    /**
     * Checks if a phase transition is valid (monotonic: SCHEDULED → LIVE → FINISHED).
     */
    public static boolean isValidTransition(MatchPhase from, MatchPhase to) {
        return switch (from) {
            case SCHEDULED -> to == MatchPhase.LIVE;
            case LIVE -> to == MatchPhase.FINISHED;
            case FINISHED -> false; // Terminal state
        };
    }

    /**
     * Checks if a match thread has timed out (48h after finished).
     * Returns true if comments should still be accepted.
     */
    public static boolean isThreadActive(MatchPhase status, Instant finishedAt, Instant now) {
        if (status == MatchPhase.SCHEDULED || status == MatchPhase.LIVE) {
            return true;
        }
        // FINISHED: allow comments for 48 hours
        if (finishedAt == null) return false;
        return Duration.between(finishedAt, now).compareTo(POST_MATCH_TIMEOUT) <= 0;
    }

    /**
     * Validates a player rating value.
     */
    public static boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 10;
    }

    /**
     * Calculates MOTM from a list of (playerId, ratings[]) tuples.
     * Returns the playerId with highest average when >= minRatings, or null.
     * Tie-break: higher count wins.
     */
    public static UUID calculateMotmFromRatings(List<PlayerRatingSummary> summaries, int minRatings) {
        return summaries.stream()
                .filter(s -> s.count() >= minRatings)
                .sorted((a, b) -> {
                    int cmp = Double.compare(b.averageRating(), a.averageRating());
                    if (cmp != 0) return cmp;
                    return Long.compare(b.count(), a.count());
                })
                .map(PlayerRatingSummary::playerId)
                .findFirst()
                .orElse(null);
    }

    public record PlayerRatingSummary(UUID playerId, double averageRating, long count) {}

    // ---- Private helpers ----

    private MatchEntity findMatchOrThrow(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("MATCH_NOT_FOUND", "Match not found"));
    }

    private void validateNotTimedOut(MatchEntity match) {
        if (!isThreadActive(match.getStatus(), match.getFinishedAt(), Instant.now())) {
            throw new BusinessRuleException("THREAD_TIMED_OUT",
                    "Match thread is closed: 48 hours after match finished");
        }
    }

    private MatchResponse toMatchResponse(MatchEntity match) {
        return new MatchResponse(
                match.getId(), match.getHomeClubId(), match.getAwayClubId(),
                match.getHomeScore(), match.getAwayScore(),
                match.getStatus(), match.getKickOffTime(), match.getFinishedAt()
        );
    }
}
