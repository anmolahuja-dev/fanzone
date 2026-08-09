package com.fanzone.auth.repository;

import com.fanzone.auth.model.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    List<PlayerEntity> findByClubId(UUID clubId);

    List<PlayerEntity> findByIdIn(List<UUID> ids);
}
