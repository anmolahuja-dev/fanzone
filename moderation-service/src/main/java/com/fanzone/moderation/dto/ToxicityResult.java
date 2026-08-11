package com.fanzone.moderation.dto;

public record ToxicityResult(
        boolean isToxic,
        String reason,
        double confidence
) {
    public static ToxicityResult safe() {
        return new ToxicityResult(false, null, 0.0);
    }

    public static ToxicityResult toxic(String reason, double confidence) {
        return new ToxicityResult(true, reason, confidence);
    }
}
