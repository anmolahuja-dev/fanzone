package com.fanzone.post.service;

import com.fanzone.common.exceptions.ForbiddenException;
import com.fanzone.post.model.CommentEntity;
import com.fanzone.post.model.CommentUpvoteEntity;
import com.fanzone.post.repository.CommentRepository;
import com.fanzone.post.repository.CommentUpvoteRepository;
import com.fanzone.post.repository.PostRepository;
import com.fanzone.post.repository.ReportRepository;
import net.jqwik.api.*;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for:
 * - Upvote idempotence (Property 8): repeated upvotes don't increment count more than once
 * - Self-upvote prevention (Property 17): user cannot upvote their own comment
 */
class CommentUpvotePropertyTest {

    // =================================================================
    // Property 8: Upvote Idempotence
    // =================================================================

    @Property(tries = 100)
    void upvote_repeatedCalls_onlyIncrementOnce(
            @ForAll("randomUUID") UUID commentId,
            @ForAll("randomUUID") UUID userId,
            @ForAll("randomUUID") UUID commentAuthorId,
            @ForAll("positiveInt") int repeatCount) {

        // Ensure user != author (not self-upvote)
        if (userId.equals(commentAuthorId)) return;

        CommentRepository commentRepo = mock(CommentRepository.class);
        CommentUpvoteRepository upvoteRepo = mock(CommentUpvoteRepository.class);
        PostRepository postRepo = mock(PostRepository.class);
        ReportRepository reportRepo = mock(ReportRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        // Setup: comment exists with different author
        CommentEntity comment = new CommentEntity();
        comment.setId(commentId);
        comment.setUserId(commentAuthorId);
        comment.setPostId(UUID.randomUUID());
        comment.setContent("test");
        comment.setCreatedAt(Instant.now());
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));

        // First call: not yet upvoted → save
        // Subsequent calls: already upvoted → no-op
        when(upvoteRepo.existsByUserIdAndCommentId(userId, commentId))
                .thenReturn(false)   // first call
                .thenReturn(true);   // all subsequent

        CommentServiceImpl service = new CommentServiceImpl(
                commentRepo, upvoteRepo, postRepo, reportRepo, kafkaTemplate);

        // Call upvote multiple times
        for (int i = 0; i < repeatCount; i++) {
            service.upvote(commentId, userId);
        }

        // Verify: save called exactly once (idempotent)
        verify(upvoteRepo, times(1)).save(any(CommentUpvoteEntity.class));
        verify(commentRepo, times(1)).incrementUpvoteCount(commentId);
    }

    // =================================================================
    // Property 17: Self-Upvote Prevention
    // =================================================================

    @Property(tries = 100)
    void upvote_selfUpvote_alwaysRejected(@ForAll("randomUUID") UUID userId) {
        UUID commentId = UUID.randomUUID();

        CommentRepository commentRepo = mock(CommentRepository.class);
        CommentUpvoteRepository upvoteRepo = mock(CommentUpvoteRepository.class);
        PostRepository postRepo = mock(PostRepository.class);
        ReportRepository reportRepo = mock(ReportRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        // Comment authored by the same user trying to upvote
        CommentEntity comment = new CommentEntity();
        comment.setId(commentId);
        comment.setUserId(userId); // same as upvoter
        comment.setPostId(UUID.randomUUID());
        comment.setContent("my own comment");
        comment.setCreatedAt(Instant.now());
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));

        CommentServiceImpl service = new CommentServiceImpl(
                commentRepo, upvoteRepo, postRepo, reportRepo, kafkaTemplate);

        // Self-upvote must always throw ForbiddenException
        boolean rejected = false;
        try {
            service.upvote(commentId, userId);
        } catch (ForbiddenException e) {
            rejected = true;
            assert "SELF_UPVOTE".equals(e.getCode()) :
                    "Expected SELF_UPVOTE code but got: " + e.getCode();
        }

        assert rejected : "Self-upvote was not rejected for user " + userId;

        // Verify: no upvote was saved, no count incremented
        verify(upvoteRepo, never()).save(any());
        verify(commentRepo, never()).incrementUpvoteCount(any());
    }

    @Property(tries = 50)
    void upvote_differentUser_accepted(
            @ForAll("randomUUID") UUID commentId,
            @ForAll("twoDistinctUUIDs") UUID[] userPair) {

        UUID authorId = userPair[0];
        UUID upvoterId = userPair[1];

        CommentRepository commentRepo = mock(CommentRepository.class);
        CommentUpvoteRepository upvoteRepo = mock(CommentUpvoteRepository.class);
        PostRepository postRepo = mock(PostRepository.class);
        ReportRepository reportRepo = mock(ReportRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        CommentEntity comment = new CommentEntity();
        comment.setId(commentId);
        comment.setUserId(authorId);
        comment.setPostId(UUID.randomUUID());
        comment.setContent("test");
        comment.setCreatedAt(Instant.now());
        when(commentRepo.findById(commentId)).thenReturn(Optional.of(comment));
        when(upvoteRepo.existsByUserIdAndCommentId(upvoterId, commentId)).thenReturn(false);

        CommentServiceImpl service = new CommentServiceImpl(
                commentRepo, upvoteRepo, postRepo, reportRepo, kafkaTemplate);

        // Should NOT throw
        service.upvote(commentId, upvoterId);

        verify(upvoteRepo, times(1)).save(any(CommentUpvoteEntity.class));
        verify(commentRepo, times(1)).incrementUpvoteCount(commentId);
    }

    // ---- Providers ----

    @Provide
    Arbitrary<UUID> randomUUID() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<Integer> positiveInt() {
        return Arbitraries.integers().between(2, 10);
    }

    @Provide
    Arbitrary<UUID[]> twoDistinctUUIDs() {
        return Arbitraries.create(() -> {
            UUID a = UUID.randomUUID();
            UUID b = UUID.randomUUID();
            return new UUID[]{a, b};
        });
    }
}
