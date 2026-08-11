package com.fanzone.matchthread.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "player_ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "user_id", "player_id"})
})
@Getter
@Setter
public class PlayerRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "rating", nullable = false)
    private int rating;
}
