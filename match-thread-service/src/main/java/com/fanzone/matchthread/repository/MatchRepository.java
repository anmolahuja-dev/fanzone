package com.fanzone.matchthread.repository;

import com.fanzone.common.enums.MatchPhase;
import com.fanzone.matchthread.model.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {

    @Query("SELECT m FROM MatchEntity m WHERE (m.homeClubId = :clubId OR m.awayClubId = :clubId) " +
            "AND m.status IN :statuses ORDER BY m.kickOffTime ASC")
    List<MatchEntity> findByClubAndStatuses(@Param("clubId") UUID clubId,
                                            @Param("statuses") List<MatchPhase> statuses);
}
