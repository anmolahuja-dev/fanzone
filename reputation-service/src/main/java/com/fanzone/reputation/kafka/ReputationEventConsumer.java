package com.fanzone.reputation.kafka;

import com.fanzone.common.enums.ReputationEvent;
import com.fanzone.common.events.CommentUpvotedEvent;
import com.fanzone.common.events.KafkaTopics;
import com.fanzone.common.events.PostUpvotedEvent;
import com.fanzone.reputation.service.ReputationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that processes reputation-affecting events.
 * <p>
 * Consumes from: fanzone.reputation.events
 * Processes PostUpvotedEvent and CommentUpvotedEvent to adjust author reputation.
 * DLQ: fanzone.reputation.events.dlq (handled by Spring Kafka error handler)
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class ReputationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReputationEventConsumer.class);

    private final ReputationService reputationService;

    public ReputationEventConsumer(ReputationService reputationService) {
        this.reputationService = reputationService;
    }

    @KafkaListener(
            topics = KafkaTopics.REPUTATION_EVENTS,
            groupId = "reputation-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onReputationEvent(
            @Payload Object event,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
            @Header(value = KafkaHeaders.OFFSET, required = false) Long offset) {

        try {
            if (event instanceof PostUpvotedEvent postUpvoted) {
                log.info("Processing PostUpvotedEvent: postId={}, authorId={}, upvoterId={}",
                        postUpvoted.postId(), postUpvoted.authorId(), postUpvoted.upvoterId());
                reputationService.adjustReputation(postUpvoted.authorId(), ReputationEvent.POST_UPVOTED);

            } else if (event instanceof CommentUpvotedEvent commentUpvoted) {
                log.info("Processing CommentUpvotedEvent: commentId={}, authorId={}, upvoterId={}",
                        commentUpvoted.commentId(), commentUpvoted.authorId(), commentUpvoted.upvoterId());
                reputationService.adjustReputation(commentUpvoted.authorId(), ReputationEvent.COMMENT_UPVOTED);

            } else {
                log.warn("Unknown event type received on {}: {}", topic, event.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("Error processing reputation event (offset={}): {}", offset, e.getMessage(), e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }
}
