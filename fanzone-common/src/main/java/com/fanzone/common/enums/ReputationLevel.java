package com.fanzone.common.enums;

public enum ReputationLevel {

    ROOKIE(0, 99),
    ACTIVE(100, 499),
    TRUSTED(500, 1999),
    CLUB_EXPERT(2000, Integer.MAX_VALUE);

    private final int minScore;
    private final int maxScore;

    ReputationLevel(int minScore, int maxScore) {
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public boolean isHighReputation() {
        return this == TRUSTED || this == CLUB_EXPERT;
    }

    public static ReputationLevel fromScore(int score) {
        if (score >= CLUB_EXPERT.minScore) return CLUB_EXPERT;
        if (score >= TRUSTED.minScore) return TRUSTED;
        if (score >= ACTIVE.minScore) return ACTIVE;
        return ROOKIE;
    }
}
