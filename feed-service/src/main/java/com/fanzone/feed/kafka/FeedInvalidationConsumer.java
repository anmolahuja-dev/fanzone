package com.fanzone.feed.kafka;

import com.fanzone.common.events.FeedInvalidationEvent;
import com.fanzone.common.events.KafkaTopics;
import com.fanzone.feed.cache.FeedCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for feed invalidation events and clears
 * the corresponding Redis feed caches.
 * <p>
 * Consumes from topic: fanzone.feed.invalidation
 * Consumer group: feed-service-invalidation
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class FeedInvalidationConsumer {

    private static final Logger log = LoggerFactory.getLogger(FeedInvalidationConsumer.class);

    private final FeedCacheService feedCacheService;

    public FeedInvalidationConsumer(FeedCacheService feedCacheService) {
        this.feedCacheService = feedCacheService;
    }

    @KafkaListener(
            topics = KafkaTopics.FEED_INVALIDATION,
            groupId = "feed-service-invalidation",
            containerFactory = "feedInvalidationListenerContainerFactory"
    )
    public void onFeedInvalidation(
            @Payload FeedInvalidationEvent event,
            @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        try {
            log.info("Received feed invalidation event: clubId={}, postId={}, reason={}",
                    event.clubId(), event.postId(), event.reason());

            if (event.clubId() != null) {
                feedCacheService.invalidateForClub(event.clubId());
            } else {
                log.warn("FeedInvalidationEvent received with null clubId, skipping invalidation. postId={}",
                        event.postId());
            }
        } catch (Exception e) {
            log.error("Error processing feed invalidation event: clubId={}, postId={}, reason={}",
                    event.clubId(), event.postId(), event.reason(), e);
            throw e; // Re-throw to let the error handler manage retries/DLQ
        }
    }
}
