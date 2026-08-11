package com.fanzone.post.service;

import com.fanzone.common.exceptions.ConflictException;
import com.fanzone.common.exceptions.NotFoundException;
import com.fanzone.common.exceptions.ValidationException;
import com.fanzone.post.dto.UserProfileResponse;
import com.fanzone.post.model.UserEntity;
import com.fanzone.post.repository.FollowRepository;
import com.fanzone.post.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png");

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ImageStorageService imageStorageService;

    public ProfileServiceImpl(UserRepository userRepository,
                              FollowRepository followRepository,
                              ImageStorageService imageStorageService) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.imageStorageService = imageStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId, UUID viewerId) {
        UserEntity user = findUserOrThrow(userId);

        long followers = followRepository.countFollowers(userId);
        long following = followRepository.countFollowing(userId);
        boolean isFollowing = viewerId != null && !viewerId.equals(userId)
                && followRepository.existsByFollowerIdAndFollowedId(viewerId, userId);

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFavoriteClubId(),
                user.getReputationLevel(),
                user.getReputation(),
                user.getProfilePictureUrl(),
                followers,
                following,
                isFollowing
        );
    }

    @Override
    public UserProfileResponse updateUsername(UUID userId, String newUsername) {
        UserEntity user = findUserOrThrow(userId);

        if (newUsername != null && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new ConflictException("USERNAME_TAKEN", "Username is already taken");
            }
            user.setUsername(newUsername);
            userRepository.save(user);
            log.info("User {} updated username to {}", userId, newUsername);
        }

        return getProfile(userId, userId);
    }

    @Override
    public String uploadAvatar(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("AVATAR_REQUIRED", "Avatar file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException("AVATAR_FORMAT_INVALID", "Avatar must be JPEG or PNG format");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new ValidationException("AVATAR_TOO_LARGE", "Avatar must not exceed 5MB");
        }

        String url = imageStorageService.uploadImage(file, "avatars");

        UserEntity user = findUserOrThrow(userId);
        user.setProfilePictureUrl(url);
        userRepository.save(user);

        log.info("User {} uploaded avatar: {}", userId, url);
        return url;
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    @Override
    public void setThemePreference(UUID userId, String theme) {
        UserEntity user = findUserOrThrow(userId);
        user.setThemePreference(theme);
        userRepository.save(user);
        log.info("User {} set theme to {}", userId, theme);
    }
}
