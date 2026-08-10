package com.fanzone.feed.scoring;

import com.fanzone.common.enums.ReputationLevel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Deterministic composite feed scoring.
 * <p>
 * This is a pure function — no side effects, no database calls, no mutable state.
 * Identical inputs always produce identical output.
 * <p>
 * Scoring rules:
 * <ul>
 *   <li>+100 if post.clubId == user.favoriteClubId</li>
 *   <li>+30  if author.reputationLevel is TRUSTED or CLUB_EXPERT</li>
 *   <li>+20  if post has activity within the last 2 hours</li>
 *   <li>+15  if post.commentCount >= 10</li>
 *   <li>-100 if post.toxicReportCount >= 3</li>
 * </ul>
 */
@Component
public final class FeedScorer {

    private static final int SAME_CLUB_BONUS = 100;
    private static final int HIGH_REPUTATION_BONUS = 30;
    private static final int RECENT_ACTIVITY_BONUS = 20;
    private static final int HIGH_COMMENTS_BONUS = 15;
    private static final int TOXIC_PENALTY = -100;

    private static final Duration RECENT_ACTIVITY_WINDOW = Duration.ofHours(2);
    private static final int HIGH_COMMENT_THRESHOLD = 10;
    private static final int TOXIC_REPORT_THRESHOLD = 3;

    /**
     * Compute the feed score for a post given individual parameters.
     *
     * @param postClubId       the club ID associated with the post (may be null)
     * @param authorLevel      the reputation level of the post author (may be null)
     * @param lastActivityAt   the timestamp of the most recent activity on the post (may be null)
     * @param commentCount     the number of comments on the post
     * @param toxicReportCount the number of toxic content reports on the post
     * @param userClubId       the user's favorite club ID (may be null)
     * @param now              the current timestamp for recency calculation
     * @return the composite feed score
     */
    public int computeScore(UUID postClubId, ReputationLevel authorLevel, Instant lastActivityAt,
                            int commentCount, int toxicReportCount, UUID userClubId, Instant now) {
        int score = 0;

        // +100 if post's club matches user's favorite club
        if (postClubId != null && postClubId.equals(userClubId)) {
            score += SAME_CLUB_BONUS;
        }

        // +30 if author has high reputation (TRUSTED or CLUB_EXPERT)
        if (authorLevel != null && authorLevel.isHighReputation()) {
            score += HIGH_REPUTATION_BONUS;
        }

        // +20 if post has activity within last 2 hours
        if (lastActivityAt != null && now != null) {
            Duration sinceLastActivity = Duration.between(lastActivityAt, now);
            if (!sinceLastActivity.isNegative() && sinceLastActivity.compareTo(RECENT_ACTIVITY_WINDOW) < 0) {
                score += RECENT_ACTIVITY_BONUS;
            }
        }

        // +15 if comment count >= 10
        if (commentCount >= HIGH_COMMENT_THRESHOLD) {
            score += HIGH_COMMENTS_BONUS;
        }

        // -100 if toxic report count >= 3
        if (toxicReportCount >= TOXIC_REPORT_THRESHOLD) {
            score += TOXIC_PENALTY;
        }

        return score;
    }

    /**
     * Overloaded convenience method accepting a PostScoreContext record.
     *
     * @param context    the post scoring context
     * @param userClubId the user's favorite club ID
     * @param now        the current timestamp
     * @return the composite feed score
     */
    public int computeScore(PostScoreContext context, UUID userClubId, Instant now) {
        return computeScore(
                context.postClubId(),
                context.authorLevel(),
                context.lastActivityAt(),
                context.commentCount(),
                context.toxicReportCount(),
                userClubId,
                now
        );
    }
}
