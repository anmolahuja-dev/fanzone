package com.fanzone.matchthread.controller;

import com.fanzone.common.security.UserPrincipal;
import com.fanzone.matchthread.dto.*;
import com.fanzone.matchthread.service.MatchThreadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for match threads.
 * <p>
 * Provides endpoints for matches, live comments (rate-limited),
 * player ratings, goal reactions, and MOTM results.
 * Comments are also broadcast via WebSocket to /topic/match-threads/{matchId}.
 */
@RestController
@RequestMapping("/api/v1")
public class MatchThreadController {

    private final MatchThreadService matchThreadService;
    private final SimpMessagingTemplate messagingTemplate;

    public MatchThreadController(MatchThreadService matchThreadService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.matchThreadService = matchThreadService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * List upcoming/live matches for the user's club.
     */
    @GetMapping("/matches")
    public ResponseEntity<List<MatchResponse>> getMatches(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<MatchResponse> matches = matchThreadService.getMatchesForClub(principal.getFavoriteClubId());
        return ResponseEntity.ok(matches);
    }

    /**
     * Get match details.
     */
    @GetMapping("/matches/{matchId}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable UUID matchId) {
        MatchResponse match = matchThreadService.getMatch(matchId);
        return ResponseEntity.ok(match);
    }

    /**
     * Get comments for a match thread (paginated, most recent first).
     */
    @GetMapping("/match-threads/{matchId}/comments")
    public ResponseEntity<List<MatchCommentResponse>> getComments(@PathVariable UUID matchId) {
        List<MatchCommentResponse> comments = matchThreadService.getComments(matchId);
        return ResponseEntity.ok(comments);
    }

    /**
     * Post a comment to a match thread.
     * Rate limited to 20 comments per user per 60 seconds.
     * Also broadcasts via WebSocket.
     */
    @PostMapping("/match-threads/{matchId}/comments")
    public ResponseEntity<MatchCommentResponse> postComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID matchId,
            @Valid @RequestBody MatchCommentRequest request) {

        MatchCommentResponse response = matchThreadService.submitComment(
                matchId, principal.getUserId(), request.content());

        // Broadcast to WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/match-threads/" + matchId, response);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submit a player rating (1-10). Upserts on resubmit.
     */
    @PostMapping("/match-threads/{matchId}/ratings")
    public ResponseEntity<Void> submitRating(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID matchId,
            @Valid @RequestBody PlayerRatingRequest request) {

        matchThreadService.submitPlayerRating(matchId, principal.getUserId(),
                request.playerId(), request.rating());
        return ResponseEntity.ok().build();
    }

    /**
     * Submit a goal reaction.
     */
    @PostMapping("/match-threads/{matchId}/reactions")
    public ResponseEntity<Void> submitReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID matchId,
            @Valid @RequestBody GoalReactionRequest request) {

        matchThreadService.submitGoalReaction(matchId, principal.getUserId(), request.reactionType());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Get MOTM results (top-3 players by average rating, min 10 ratings required).
     */
    @GetMapping("/match-threads/{matchId}/motm")
    public ResponseEntity<List<MotmResponse>> getMotm(@PathVariable UUID matchId) {
        List<MotmResponse> motm = matchThreadService.calculateMotm(matchId);
        return ResponseEntity.ok(motm);
    }
}
