package com.fanzone.reputation.service;

import com.fanzone.common.enums.ReputationEvent;
import com.fanzone.common.enums.ReputationLevel;

import java.util.UUID;

public interface ReputationService {

    /**
     * Adjusts a user's reputation based on an event.
     * Updates the score (clamped to >= 0) and recalculates the level.
     * Publishes a ReputationChangedEvent if the level changes.
     *
     * @param userId the user whose reputation to adjust
     * @param event  the reputation event type
     */
    void adjustReputation(UUID userId, ReputationEvent event);

    /**
     * Computes the reputation level for a given score.
     * Pure function — no side effects.
     *
     * @param score the reputation score
     * @return the corresponding level
     */
    ReputationLevel computeLevel(int score);

    /**
     * Adjusts a score by a delta, clamped to minimum 0.
     * Pure function — no side effects.
     *
     * @param currentScore the current score
     * @param delta        the points to add (can be negative)
     * @return the new score, never below 0
     */
    int adjustScore(int currentScore, int delta);
}
