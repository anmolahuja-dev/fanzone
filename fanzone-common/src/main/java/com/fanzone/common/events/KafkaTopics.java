package com.fanzone.common.events;

/**
 * Kafka topic name constants used across all services.
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String REPUTATION_EVENTS = "fanzone.reputation.events";
    public static final String NOTIFICATION_EVENTS = "fanzone.notification.events";
    public static final String MODERATION_REQUESTS = "fanzone.moderation.requests";
    public static final String MATCH_EVENTS = "fanzone.match.events";
    public static final String FEED_INVALIDATION = "fanzone.feed.invalidation";

    // Dead Letter Queues
    public static final String REPUTATION_EVENTS_DLQ = "fanzone.reputation.events.dlq";
    public static final String NOTIFICATION_EVENTS_DLQ = "fanzone.notification.events.dlq";
    public static final String MODERATION_REQUESTS_DLQ = "fanzone.moderation.requests.dlq";
    public static final String MATCH_EVENTS_DLQ = "fanzone.match.events.dlq";
    public static final String FEED_INVALIDATION_DLQ = "fanzone.feed.invalidation.dlq";
}
