package com.fanzone.reputation.service;

import com.fanzone.common.enums.ReputationEvent;
import com.fanzone.common.enums.ReputationLevel;
import com.fanzone.common.events.KafkaTopics;
import com.fanzone.common.events.ReputationChangedEvent;
import com.fanzone.reputation.model.UserReputationEntity;
import com.fanzone.reputation.repository.UserReputationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class ReputationServiceImpl implements ReputationService {

    private static final Logger log = LoggerFactory.getLogger(ReputationServiceImpl.class);

    private final UserReputationRepository userReputationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReputationServiceImpl(UserReputationRepository userReputationRepository,
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.userReputationRepository = userReputationRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void adjustReputation(UUID userId, ReputationEvent event) {
        UserReputationEntity user = userReputationRepository.findById(userId)
                .orElseGet(() -> {
                    log.warn("User {} not found for reputation adjustment, skipping", userId);
                    return null;
                });

        if (user == null) {
            return;
        }

        int previousScore = user.getReputation();
        ReputationLevel previousLevel = user.getReputationLevel();

        // Compute new score (clamped to >= 0)
        int newScore = adjustScore(previousScore, event.getPoints());
        ReputationLevel newLevel = computeLevel(newScore);

        // Update entity
        user.setReputation(newScore);
        user.setReputationLevel(newLevel);
        userReputationRepository.save(user);

        log.info("Reputation adjusted for user {}: {} -> {} (event: {}, level: {} -> {})",
                userId, previousScore, newScore, event, previousLevel, newLevel);

        // Publish level change event if level changed
        if (previousLevel != newLevel) {
            publishLevelChangeEvent(userId, previousScore, newScore, previousLevel, newLevel, event);
        }
    }

    @Override
    public ReputationLevel computeLevel(int score) {
        return ReputationLevel.fromScore(score);
    }

    @Override
    public int adjustScore(int currentScore, int delta) {
        return Math.max(0, currentScore + delta);
    }

    private void publishLevelChangeEvent(UUID userId, int previousScore, int newScore,
                                         ReputationLevel previousLevel, ReputationLevel newLevel,
                                         ReputationEvent event) {
        try {
            ReputationChangedEvent changedEvent = new ReputationChangedEvent(
                    userId,
                    previousScore,
                    newScore,
                    previousLevel.name(),
                    newLevel.name(),
                    event.name(),
                    Instant.now()
            );
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, userId.toString(), changedEvent);
            log.info("Published level change event for user {}: {} -> {}", userId, previousLevel, newLevel);
        } catch (Exception e) {
            log.warn("Failed to publish ReputationChangedEvent for user {}: {}", userId, e.getMessage());
        }
    }
}
