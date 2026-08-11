package com.fanzone.matchthread.repository;

import com.fanzone.matchthread.model.GoalReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GoalReactionRepository extends JpaRepository<GoalReactionEntity, UUID> {
}
