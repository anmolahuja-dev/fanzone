package com.fanzone.auth.repository;

import com.fanzone.auth.model.UserInterest;
import com.fanzone.auth.model.UserInterestId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, UserInterestId> {

    void deleteByUserId(UUID userId);
}
