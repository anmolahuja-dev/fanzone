package com.fanzone.post.service;

import java.util.UUID;

public interface FollowService {

    void follow(UUID followerId, UUID followedId);

    void unfollow(UUID followerId, UUID followedId);

    long getFollowerCount(UUID userId);

    long getFollowingCount(UUID userId);

    boolean isFollowing(UUID followerId, UUID followedId);
}
