package com.fanzone.post.service;

import com.fanzone.common.exceptions.ForbiddenException;
import com.fanzone.post.model.FollowEntity;
import com.fanzone.post.repository.FollowRepository;
import net.jqwik.api.*;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for follow integrity (Property 20).
 * <p>
 * Verifies:
 * - Self-follow always prevented
 * - At most one relationship between any user pair (idempotent creation)
 * - Different user pairs always succeed
 */
class FollowIntegrityPropertyTest {

    // =================================================================
    // Self-follow always prevented
    // =================================================================

    @Property(tries = 100)
    void selfFollow_alwaysRejected(@ForAll("randomUUID") UUID userId) {
        FollowRepository followRepo = mock(FollowRepository.class);
        FollowServiceImpl service = new FollowServiceImpl(followRepo);

        boolean rejected = false;
        try {
            service.follow(userId, userId);
        } catch (ForbiddenException e) {
            rejected = true;
            assert "SELF_FOLLOW".equals(e.getCode()) :
                    "Expected SELF_FOLLOW code but got: " + e.getCode();
        }

        assert rejected : "Self-follow was not rejected for user " + userId;
        verify(followRepo, never()).save(any());
    }

    @Property(tries = 100)
    void selfFollow_pureValidation_returnsFalse(@ForAll("randomUUID") UUID userId) {
        assert !FollowServiceImpl.isFollowAllowed(userId, userId) :
                "isFollowAllowed should return false for self-follow";
    }

    // =================================================================
    // Idempotent creation — duplicate follow is no-op
    // =================================================================

    @Property(tries = 100)
    void duplicateFollow_onlySavesOnce(@ForAll("twoDistinctUUIDs") UUID[] pair) {
        UUID followerId = pair[0];
        UUID followedId = pair[1];

        FollowRepository followRepo = mock(FollowRepository.class);
        // First call: not following. Second call: already following.
        when(followRepo.existsByFollowerIdAndFollowedId(followerId, followedId))
                .thenReturn(false)
                .thenReturn(true);

        FollowServiceImpl service = new FollowServiceImpl(followRepo);

        // Call follow twice
        service.follow(followerId, followedId);
        service.follow(followerId, followedId);

        // Save should only be called once
        verify(followRepo, times(1)).save(any(FollowEntity.class));
    }

    // =================================================================
    // Different user pairs always succeed
    // =================================================================

    @Property(tries = 100)
    void differentUsers_followAllowed(@ForAll("twoDistinctUUIDs") UUID[] pair) {
        UUID followerId = pair[0];
        UUID followedId = pair[1];

        assert FollowServiceImpl.isFollowAllowed(followerId, followedId) :
                "isFollowAllowed should return true for different users";
    }

    @Property(tries = 100)
    void differentUsers_followSucceeds(@ForAll("twoDistinctUUIDs") UUID[] pair) {
        UUID followerId = pair[0];
        UUID followedId = pair[1];

        FollowRepository followRepo = mock(FollowRepository.class);
        when(followRepo.existsByFollowerIdAndFollowedId(followerId, followedId)).thenReturn(false);

        FollowServiceImpl service = new FollowServiceImpl(followRepo);
        service.follow(followerId, followedId);

        verify(followRepo, times(1)).save(any(FollowEntity.class));
    }

    // =================================================================
    // Unfollow is idempotent (no error if not following)
    // =================================================================

    @Property(tries = 50)
    void unfollow_whenNotFollowing_noError(@ForAll("twoDistinctUUIDs") UUID[] pair) {
        UUID followerId = pair[0];
        UUID followedId = pair[1];

        FollowRepository followRepo = mock(FollowRepository.class);
        when(followRepo.existsByFollowerIdAndFollowedId(followerId, followedId)).thenReturn(false);

        FollowServiceImpl service = new FollowServiceImpl(followRepo);
        // Should not throw
        service.unfollow(followerId, followedId);

        verify(followRepo, never()).deleteByFollowerIdAndFollowedId(any(), any());
    }

    // ---- Providers ----

    @Provide
    Arbitrary<UUID> randomUUID() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<UUID[]> twoDistinctUUIDs() {
        return Arbitraries.create(() -> new UUID[]{UUID.randomUUID(), UUID.randomUUID()});
    }
}
