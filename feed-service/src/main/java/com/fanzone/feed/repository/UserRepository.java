package com.fanzone.feed.repository;

import com.fanzone.feed.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for user data needed by feed enrichment.
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    List<UserEntity> findAllByIdIn(List<UUID> ids);
}
