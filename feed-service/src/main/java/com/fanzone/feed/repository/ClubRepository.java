package com.fanzone.feed.repository;

import com.fanzone.feed.model.ClubEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClubRepository extends JpaRepository<ClubEntity, UUID> {
}
