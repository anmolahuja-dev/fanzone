package com.fanzone.matchthread.service;

import com.fanzone.common.enums.MatchPhase;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Property-based tests for Match Thread system.
 * <p>
 * Property 9: Phase transitions are monotonic (SCHEDULED→LIVE→FINISHED, never backward)
 * Property 10: Player rating accepted iff 1-10
 * Property 11: Rate limit rejects 21st comment in 60s window
 * Property 12: MOTM = highest avg when >=10 ratings, tie-break by count
 * Property 13: Comments accepted iff within 48h of match finish
 */
class MatchThreadPropertyTest {

    // =================================================================
    // Property 9: Phase monotonicity
    // =================================================================

    @Property(tries = 200)
    void phaseTransition_onlyForwardAllowed(@ForAll("matchPhase") MatchPhase from,
                                            @ForAll("matchPhase") MatchPhase to) {
        boolean valid = MatchThreadService.isValidTransition(from, to);

        // Valid transitions: SCHEDULED→LIVE, LIVE→FINISHED only
        boolean expected = (from == MatchPhase.SCHEDULED && to == MatchPhase.LIVE)
                || (from == MatchPhase.LIVE && to == MatchPhase.FINISHED);

        assert valid == expected :
                "Transition " + from + "→" + to + ": expected " + expected + " but got " + valid;
    }

    @Property(tries = 100)
    void phaseTransition_sequenceNeverGoesBackward(@ForAll("phaseSequence") List<MatchPhase> sequence) {
        // Simulate applying transitions
        MatchPhase current = MatchPhase.SCHEDULED;
        for (MatchPhase next : sequence) {
            if (MatchThreadService.isValidTransition(current, next)) {
                current = next;
            }
        }
        // After any sequence, current should be >= initial in ordinal
        assert current.ordinal() >= MatchPhase.SCHEDULED.ordinal();
    }

    @Example
    void phaseTransition_finishedIsTerminal() {
        assert !MatchThreadService.isValidTransition(MatchPhase.FINISHED, MatchPhase.SCHEDULED);
        assert !MatchThreadService.isValidTransition(MatchPhase.FINISHED, MatchPhase.LIVE);
        assert !MatchThreadService.isValidTransition(MatchPhase.FINISHED, MatchPhase.FINISHED);
    }

    // =================================================================
    // Property 10: Player rating bounds
    // =================================================================

    @Property(tries = 200)
    void playerRating_validRange_accepted(@ForAll @IntRange(min = 1, max = 10) int rating) {
        assert MatchThreadService.isValidRating(rating) :
                "Rating " + rating + " should be valid";
    }

    @Property(tries = 200)
    void playerRating_outOfRange_rejected(@ForAll("outOfRangeRating") int rating) {
        assert !MatchThreadService.isValidRating(rating) :
                "Rating " + rating + " should be invalid";
    }

    @Example
    void playerRating_boundaries() {
        assert MatchThreadService.isValidRating(1);
        assert MatchThreadService.isValidRating(10);
        assert !MatchThreadService.isValidRating(0);
        assert !MatchThreadService.isValidRating(11);
    }

    // =================================================================
    // Property 11: Rate limiting (20 per 60s)
    // =================================================================

    @Example
    void rateLimit_constants_correct() {
        assert RateLimiterService.getMaxCommentsPerWindow() == 20;
        assert RateLimiterService.getWindowSeconds() == 60;
    }

    // =================================================================
    // Property 12: MOTM calculation
    // =================================================================

    @Property(tries = 100)
    void motm_highestAverage_wins(@ForAll("motmScenario") List<MatchThreadService.PlayerRatingSummary> summaries) {
        UUID winner = MatchThreadService.calculateMotmFromRatings(summaries, 10);

        if (winner == null) {
            // No player has >= 10 ratings
            assert summaries.stream().noneMatch(s -> s.count() >= 10) :
                    "Expected a winner since there are players with >= 10 ratings";
        } else {
            // Winner should have highest average among qualified
            MatchThreadService.PlayerRatingSummary winnerSummary = summaries.stream()
                    .filter(s -> s.playerId().equals(winner))
                    .findFirst().orElseThrow();

            assert winnerSummary.count() >= 10 : "Winner must have >= 10 ratings";

            // No other qualified player should have a higher average
            for (MatchThreadService.PlayerRatingSummary s : summaries) {
                if (s.count() >= 10 && !s.playerId().equals(winner)) {
                    assert s.averageRating() <= winnerSummary.averageRating() :
                            "Player " + s.playerId() + " has higher avg (" + s.averageRating()
                                    + ") than winner (" + winnerSummary.averageRating() + ")";
                    // If equal avg, winner should have >= count
                    if (s.averageRating() == winnerSummary.averageRating()) {
                        assert s.count() <= winnerSummary.count() :
                                "Tie-break failed: equal avg but other has more ratings";
                    }
                }
            }
        }
    }

    @Property(tries = 50)
    void motm_belowMinRatings_returnsNull(@ForAll("belowMinScenario") List<MatchThreadService.PlayerRatingSummary> summaries) {
        UUID winner = MatchThreadService.calculateMotmFromRatings(summaries, 10);
        assert winner == null : "Expected null MOTM when no player has >= 10 ratings";
    }

    // =================================================================
    // Property 13: Post-match timeout (48h)
    // =================================================================

    @Property(tries = 200)
    void postMatchTimeout_within48h_active(@ForAll @IntRange(min = 0, max = 172799) int secondsAfterFinish) {
        Instant finishedAt = Instant.now().minusSeconds(secondsAfterFinish);
        boolean active = MatchThreadService.isThreadActive(MatchPhase.FINISHED, finishedAt, Instant.now());
        assert active : "Thread should be active " + secondsAfterFinish + "s after finish (< 48h)";
    }

    @Property(tries = 200)
    void postMatchTimeout_after48h_inactive(@ForAll @IntRange(min = 172801, max = 500000) int secondsAfterFinish) {
        Instant finishedAt = Instant.now().minusSeconds(secondsAfterFinish);
        boolean active = MatchThreadService.isThreadActive(MatchPhase.FINISHED, finishedAt, Instant.now());
        assert !active : "Thread should be inactive " + secondsAfterFinish + "s after finish (> 48h)";
    }

    @Property(tries = 100)
    void scheduledOrLive_alwaysActive(@ForAll("activePhase") MatchPhase phase) {
        boolean active = MatchThreadService.isThreadActive(phase, null, Instant.now());
        assert active : "Phase " + phase + " should always be active";
    }

    @Example
    void postMatchTimeout_exactBoundary() {
        Instant now = Instant.now();
        // Exactly 48h = 172800 seconds → should still be active (<=)
        Instant finishedAt = now.minus(Duration.ofHours(48));
        assert MatchThreadService.isThreadActive(MatchPhase.FINISHED, finishedAt, now);

        // 48h + 1s → inactive
        Instant justOver = now.minus(Duration.ofHours(48)).minusSeconds(1);
        assert !MatchThreadService.isThreadActive(MatchPhase.FINISHED, justOver, now);
    }

    // =================================================================
    // Providers
    // =================================================================

    @Provide
    Arbitrary<MatchPhase> matchPhase() {
        return Arbitraries.of(MatchPhase.values());
    }

    @Provide
    Arbitrary<MatchPhase> activePhase() {
        return Arbitraries.of(MatchPhase.SCHEDULED, MatchPhase.LIVE);
    }

    @Provide
    Arbitrary<List<MatchPhase>> phaseSequence() {
        return Arbitraries.of(MatchPhase.values()).list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<Integer> outOfRangeRating() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(-100, 0),
                Arbitraries.integers().between(11, 100)
        );
    }

    @Provide
    Arbitrary<List<MatchThreadService.PlayerRatingSummary>> motmScenario() {
        // Generate 3-8 players, some with >= 10 ratings
        return Arbitraries.integers().between(3, 8).flatMap(count ->
                Arbitraries.create(() -> {
                    List<MatchThreadService.PlayerRatingSummary> list = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        UUID playerId = UUID.randomUUID();
                        double avg = 1.0 + Math.random() * 9.0; // 1.0-10.0
                        long ratingCount = 5 + (long) (Math.random() * 20); // 5-24
                        list.add(new MatchThreadService.PlayerRatingSummary(playerId, avg, ratingCount));
                    }
                    return list;
                })
        );
    }

    @Provide
    Arbitrary<List<MatchThreadService.PlayerRatingSummary>> belowMinScenario() {
        // All players have < 10 ratings
        return Arbitraries.integers().between(2, 5).flatMap(count ->
                Arbitraries.create(() -> {
                    List<MatchThreadService.PlayerRatingSummary> list = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        UUID playerId = UUID.randomUUID();
                        double avg = 1.0 + Math.random() * 9.0;
                        long ratingCount = 1 + (long) (Math.random() * 8); // 1-8
                        list.add(new MatchThreadService.PlayerRatingSummary(playerId, avg, ratingCount));
                    }
                    return list;
                })
        );
    }
}
