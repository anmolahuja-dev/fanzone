package com.fanzone.auth.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_favorite_players")
@IdClass(UserFavoritePlayerId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavoritePlayer {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "player_id")
    private UUID playerId;
}
