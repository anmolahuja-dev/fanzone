package com.fanzone.auth.repository;

import com.fanzone.auth.model.ClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClubRepository extends JpaRepository<ClubEntity, UUID> {

    List<ClubEntity> findByNameContainingIgnoreCase(String name);
}
