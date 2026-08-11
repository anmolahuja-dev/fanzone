package com.fanzone.post.model;

import com.fanzone.common.enums.ReputationLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * User entity for post-service (read/write for profile operations).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {

    @Id
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "favorite_club_id")
    private UUID favoriteClubId;

    @Column(name = "reputation")
    private int reputation = 0;

    @Column(name = "reputation_level")
    @Enumerated(EnumType.STRING)
    private ReputationLevel reputationLevel = ReputationLevel.ROOKIE;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "theme_preference")
    private String themePreference = "system";
}
