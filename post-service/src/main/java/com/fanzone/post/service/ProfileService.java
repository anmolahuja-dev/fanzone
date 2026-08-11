package com.fanzone.post.service;

import com.fanzone.post.dto.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProfileService {

    UserProfileResponse getProfile(UUID userId, UUID viewerId);

    UserProfileResponse updateUsername(UUID userId, String newUsername);

    String uploadAvatar(UUID userId, MultipartFile file);

    void setThemePreference(UUID userId, String theme);
}
