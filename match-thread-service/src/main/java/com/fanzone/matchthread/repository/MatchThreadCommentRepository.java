package com.fanzone.matchthread.repository;

import com.fanzone.matchthread.model.MatchThreadCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchThreadCommentRepository extends JpaRepository<MatchThreadCommentEntity, UUID> {

    List<MatchThreadCommentEntity> findByMatchIdOrderByCreatedAtDesc(UUID matchId);
}
