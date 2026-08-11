package com.fanzone.reputation.repository;

import com.fanzone.reputation.model.UserReputationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserReputationRepository extends JpaRepository<UserReputationEntity, UUID> {
}
