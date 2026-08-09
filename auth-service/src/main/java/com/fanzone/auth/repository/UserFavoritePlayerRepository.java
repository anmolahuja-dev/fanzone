package com.fanzone.auth.repository;

import com.fanzone.auth.model.UserFavoritePlayer;
import com.fanzone.auth.model.UserFavoritePlayerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserFavoritePlayerRepository extends JpaRepository<UserFavoritePlayer, UserFavoritePlayerId> {

    void deleteByUserId(UUID userId);
}
