package com.fanzone.matchthread.repository;

import com.fanzone.matchthread.model.PlayerRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerRatingRepository extends JpaRepository<PlayerRatingEntity, UUID> {

    Optional<PlayerRatingEntity> findByMatchIdAndUserIdAndPlayerId(UUID matchId, UUID userId, UUID playerId);

    @Query("SELECT r.playerId, AVG(r.rating), COUNT(r) FROM PlayerRatingEntity r " +
            "WHERE r.matchId = :matchId GROUP BY r.playerId HAVING COUNT(r) >= :minRatings " +
            "ORDER BY AVG(r.rating) DESC, COUNT(r) DESC")
    List<Object[]> findTopRatedPlayers(@Param("matchId") UUID matchId, @Param("minRatings") long minRatings);
}
