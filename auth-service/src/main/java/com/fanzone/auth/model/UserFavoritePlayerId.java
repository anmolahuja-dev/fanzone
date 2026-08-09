package com.fanzone.auth.model;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserFavoritePlayerId implements Serializable {

    private UUID userId;
    private UUID playerId;
}
