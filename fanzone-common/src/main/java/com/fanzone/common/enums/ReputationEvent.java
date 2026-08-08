package com.fanzone.common.enums;

public enum ReputationEvent {

    POST_UPVOTED(2),
    COMMENT_UPVOTED(1),
    HELPFUL_BADGE(10),
    ACCURATE_PREDICTION(20),
    POST_UPVOTE_REMOVED(-2),
    COMMENT_UPVOTE_REMOVED(-1),
    COMMENT_REMOVED_BY_MOD(-20),
    TOXIC_CONTENT_CONFIRMED(-50),
    FAKE_NEWS_CONFIRMED(-100);

    private final int points;

    ReputationEvent(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }
}
