package com.fanzone.notification.repository;

import com.fanzone.notification.model.NotificationPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, UUID> {
}
