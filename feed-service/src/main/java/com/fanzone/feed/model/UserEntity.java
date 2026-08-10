package com.fanzone.feed.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import com.fanzone.common.enums.ReputationLevel;

import java.util.UUID;

/**
 * Read-only JPA entity for user data needed by feed enrichment.
 * Maps only the columns required for feed display (username, reputation, avatar).
 */
@Entity
@Table(name = "users")
@Immutable
public class UserEntity {

    @Id
    private UUID id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "reputation_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReputationLevel reputationLevel;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "favorite_club_id")
    private UUID favoriteClubId;

    protected UserEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public ReputationLevel getReputationLevel() {
        return reputationLevel;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public UUID getFavoriteClubId() {
        return favoriteClubId;
    }
}
