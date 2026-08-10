package com.fanzone.post.repository;

import com.fanzone.post.model.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(UUID reporterId, String targetType, UUID targetId);
}
