package com.fanzone.reputation.service;

import com.fanzone.common.enums.ReputationEvent;
import com.fanzone.common.enums.ReputationLevel;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.List;

/**
 * Property-based tests for the Reputation system.
 * <p>
 * Property 14: Final score equals sum of event points clamped to >= 0
 * Property 15: Score never goes below 0 (floor invariant)
 * Property 16: Level assignment matches threshold boundaries
 */
class ReputationPropertyTest {

    private final ReputationServiceImpl service = new ReputationServiceImpl(null, null);

    // =================================================================
    // Property 14: Reputation events — final score equals sum clamped to >= 0
    // =================================================================

    @Property(tries = 200)
    void reputationEvents_finalScoreEqualsClampedSum(
            @ForAll @IntRange(min = 0, max = 5000) int startScore,
            @ForAll("randomEventSequence") List<ReputationEvent> events) {

        int score = startScore;
        for (ReputationEvent event : events) {
            score = service.adjustScore(score, event.getPoints());
        }

        // Verify: equivalent to Math.max(0, startScore + sum(points))
        int rawSum = startScore;
        for (ReputationEvent event : events) {
            rawSum += event.getPoints();
            rawSum = Math.max(0, rawSum); // Clamped at each step
        }

        assert score == rawSum :
                "Score " + score + " != expected " + rawSum + " after " + events.size() + " events from start " + startScore;
    }

    @Property(tries = 100)
    void reputationEvents_singleEvent_correctDelta(
            @ForAll @IntRange(min = 0, max = 3000) int startScore,
            @ForAll("singleEvent") ReputationEvent event) {

        int newScore = service.adjustScore(startScore, event.getPoints());
        int expected = Math.max(0, startScore + event.getPoints());

        assert newScore == expected :
                "adjustScore(" + startScore + ", " + event.getPoints() + ") = " + newScore + ", expected " + expected;
    }

    // =================================================================
    // Property 15: Reputation floor — score never goes below 0
    // =================================================================

    @Property(tries = 300)
    void reputationFloor_neverBelowZero(
            @ForAll @IntRange(min = 0, max = 10000) int startScore,
            @ForAll("heavyNegativeSequence") List<ReputationEvent> events) {

        int score = startScore;
        for (ReputationEvent event : events) {
            score = service.adjustScore(score, event.getPoints());
            assert score >= 0 :
                    "Score went below 0: " + score + " after applying " + event + " (points=" + event.getPoints() + ")";
        }
    }

    @Property(tries = 100)
    void reputationFloor_fromZero_negativeEvents_stayAtZero(
            @ForAll("heavyNegativeSequence") List<ReputationEvent> events) {

        int score = 0;
        for (ReputationEvent event : events) {
            score = service.adjustScore(score, event.getPoints());
            assert score >= 0 : "Score below 0 from initial 0";
        }
        // With only negative events from 0, score should always be 0
        assert score == 0 : "Score should remain 0 with negative events from 0, got " + score;
    }

    @Example
    void reputationFloor_maxNegativeFromLowScore() {
        // FAKE_NEWS_CONFIRMED = -100, from score 50 → should be 0
        int result = service.adjustScore(50, ReputationEvent.FAKE_NEWS_CONFIRMED.getPoints());
        assert result == 0 : "Expected 0 but got " + result;
    }

    // =================================================================
    // Property 16: Level assignment — correct level per score threshold
    // =================================================================

    @Property(tries = 500)
    void levelAssignment_correctForScore(@ForAll @IntRange(min = 0, max = 5000) int score) {
        ReputationLevel level = service.computeLevel(score);

        if (score >= 2000) {
            assert level == ReputationLevel.CLUB_EXPERT :
                    "Score " + score + " should be CLUB_EXPERT, got " + level;
        } else if (score >= 500) {
            assert level == ReputationLevel.TRUSTED :
                    "Score " + score + " should be TRUSTED, got " + level;
        } else if (score >= 100) {
            assert level == ReputationLevel.ACTIVE :
                    "Score " + score + " should be ACTIVE, got " + level;
        } else {
            assert level == ReputationLevel.ROOKIE :
                    "Score " + score + " should be ROOKIE, got " + level;
        }
    }

    @Property(tries = 200)
    void levelAssignment_withinBounds(@ForAll @IntRange(min = 0, max = 5000) int score) {
        ReputationLevel level = service.computeLevel(score);
        assert score >= level.getMinScore() :
                "Score " + score + " below level " + level + " min " + level.getMinScore();
        assert score <= level.getMaxScore() :
                "Score " + score + " above level " + level + " max " + level.getMaxScore();
    }

    // --- Boundary tests ---

    @Example
    void levelBoundary_99_isRookie() {
        assert service.computeLevel(99) == ReputationLevel.ROOKIE;
    }

    @Example
    void levelBoundary_100_isActive() {
        assert service.computeLevel(100) == ReputationLevel.ACTIVE;
    }

    @Example
    void levelBoundary_499_isActive() {
        assert service.computeLevel(499) == ReputationLevel.ACTIVE;
    }

    @Example
    void levelBoundary_500_isTrusted() {
        assert service.computeLevel(500) == ReputationLevel.TRUSTED;
    }

    @Example
    void levelBoundary_1999_isTrusted() {
        assert service.computeLevel(1999) == ReputationLevel.TRUSTED;
    }

    @Example
    void levelBoundary_2000_isClubExpert() {
        assert service.computeLevel(2000) == ReputationLevel.CLUB_EXPERT;
    }

    @Example
    void levelBoundary_0_isRookie() {
        assert service.computeLevel(0) == ReputationLevel.ROOKIE;
    }

    // =================================================================
    // Providers
    // =================================================================

    @Provide
    Arbitrary<List<ReputationEvent>> randomEventSequence() {
        return Arbitraries.of(ReputationEvent.values()).list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<ReputationEvent> singleEvent() {
        return Arbitraries.of(ReputationEvent.values());
    }

    @Provide
    Arbitrary<List<ReputationEvent>> heavyNegativeSequence() {
        // Bias toward negative events
        return Arbitraries.of(
                ReputationEvent.POST_UPVOTE_REMOVED,
                ReputationEvent.COMMENT_UPVOTE_REMOVED,
                ReputationEvent.COMMENT_REMOVED_BY_MOD,
                ReputationEvent.TOXIC_CONTENT_CONFIRMED,
                ReputationEvent.FAKE_NEWS_CONFIRMED
        ).list().ofMinSize(1).ofMaxSize(30);
    }
}
