package com.fanzone.post.controller;

import com.fanzone.common.security.UserPrincipal;
import com.fanzone.post.dto.SetThemeRequest;
import com.fanzone.post.dto.UpdateProfileRequest;
import com.fanzone.post.dto.UserProfileResponse;
import com.fanzone.post.service.FollowService;
import com.fanzone.post.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for user profiles and follow operations.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final ProfileService profileService;
    private final FollowService followService;

    public UserController(ProfileService profileService, FollowService followService) {
        this.profileService = profileService;
        this.followService = followService;
    }

    /**
     * Get the current user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        UserProfileResponse profile = profileService.getProfile(principal.getUserId(), principal.getUserId());
        return ResponseEntity.ok(profile);
    }

    /**
     * Update the current user's username.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse profile = profileService.updateUsername(principal.getUserId(), request.username());
        return ResponseEntity.ok(profile);
    }

    /**
     * Upload a profile picture (JPEG/PNG, max 5MB).
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file) {

        String url = profileService.uploadAvatar(principal.getUserId(), file);
        return ResponseEntity.ok(url);
    }

    /**
     * Get another user's profile.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {

        UserProfileResponse profile = profileService.getProfile(userId, principal.getUserId());
        return ResponseEntity.ok(profile);
    }

    /**
     * Follow a user. Idempotent — repeated calls are no-ops.
     * Self-follow returns 403.
     */
    @PostMapping("/{userId}/follows")
    public ResponseEntity<Void> follow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {

        followService.follow(principal.getUserId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Unfollow a user. No-op if not currently following.
     */
    @DeleteMapping("/{userId}/follows")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID userId) {

        followService.unfollow(principal.getUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set theme preference (light, dark, or system).
     */
    @PutMapping("/me/theme")
    public ResponseEntity<Void> setTheme(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SetThemeRequest request) {

        profileService.setThemePreference(principal.getUserId(), request.theme());
        return ResponseEntity.ok().build();
    }
}
