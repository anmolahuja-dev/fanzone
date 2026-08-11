package com.fanzone.notification.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
public class NotificationPreferenceEntity {

    @Id
    private UUID userId;

    @Column(name = "match_alerts")
    private boolean matchAlerts = true;

    @Column(name = "goals")
    private boolean goals = true;

    @Column(name = "replies")
    private boolean replies = true;

    @Column(name = "mentions")
    private boolean mentions = true;
}
