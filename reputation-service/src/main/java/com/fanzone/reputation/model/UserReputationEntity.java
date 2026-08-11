package com.fanzone.reputation.model;

import com.fanzone.common.enums.ReputationLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Tracks a user's reputation score and level.
 * This is a projection of the users table (reputation columns only),
 * or a dedicated table if reputation is stored separately.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class UserReputationEntity {

    @Id
    private UUID id;

    @Column(name = "reputation", nullable = false)
    private int reputation = 0;

    @Column(name = "reputation_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReputationLevel reputationLevel = ReputationLevel.ROOKIE;
}
