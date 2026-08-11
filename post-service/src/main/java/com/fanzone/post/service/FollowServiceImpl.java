package com.fanzone.post.service;

import com.fanzone.common.exceptions.ForbiddenException;
import com.fanzone.post.model.FollowEntity;
import com.fanzone.post.repository.FollowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Follow system implementation.
 * <p>
 * Constraints:
 * - Self-follow prevented (403)
 * - Idempotent: duplicate follow → no error, no duplicate row
 * - No notification on follow (anti-follower-farming)
 */
@Service
@Transactional
public class FollowServiceImpl implements FollowService {

    private static final Logger log = LoggerFactory.getLogger(FollowServiceImpl.class);

    private final FollowRepository followRepository;

    public FollowServiceImpl(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Override
    public void follow(UUID followerId, UUID followedId) {
        // Prevent self-follow
        if (followerId.equals(followedId)) {
            throw new ForbiddenException("SELF_FOLLOW", "You cannot follow yourself");
        }

        // Idempotent — if already following, no-op
        if (followRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            return;
        }

        FollowEntity follow = new FollowEntity();
        follow.setFollowerId(followerId);
        follow.setFollowedId(followedId);
        followRepository.save(follow);

        log.info("User {} followed user {}", followerId, followedId);
        // Note: No notification published (anti-follower-farming design decision)
    }

    @Override
    public void unfollow(UUID followerId, UUID followedId) {
        if (!followRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            return; // Not following, no-op
        }

        followRepository.deleteByFollowerIdAndFollowedId(followerId, followedId);
        log.info("User {} unfollowed user {}", followerId, followedId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getFollowerCount(UUID userId) {
        return followRepository.countFollowers(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getFollowingCount(UUID userId) {
        return followRepository.countFollowing(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followedId) {
        return followRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    // --- Pure logic exposed for property testing ---

    /**
     * Validates whether a follow operation is allowed (not self-follow).
     */
    public static boolean isFollowAllowed(UUID followerId, UUID followedId) {
        return !followerId.equals(followedId);
    }
}
