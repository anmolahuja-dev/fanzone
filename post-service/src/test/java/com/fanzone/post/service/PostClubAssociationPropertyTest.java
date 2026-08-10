package com.fanzone.post.service;

import com.fanzone.common.enums.PostType;
import com.fanzone.post.dto.CreatePostRequest;
import com.fanzone.post.dto.PostResponse;
import com.fanzone.post.model.PostEntity;
import com.fanzone.post.repository.PostRepository;
import com.fanzone.post.repository.PostUpvoteRepository;
import com.fanzone.post.repository.ReportRepository;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for post club association (Property 6).
 * <p>
 * Verifies that every created post's clubId always equals the author's favoriteClubId.
 */
class PostClubAssociationPropertyTest {

    @Property(tries = 100)
    void createdPost_alwaysHasAuthorsClub(
            @ForAll("randomUserId") UUID userId,
            @ForAll("randomClubId") UUID userClubId,
            @ForAll("randomContent") String content) {

        // Mock dependencies
        PostRepository postRepository = mock(PostRepository.class);
        PostUpvoteRepository postUpvoteRepository = mock(PostUpvoteRepository.class);
        ReportRepository reportRepository = mock(ReportRepository.class);
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        // Capture the saved entity to verify clubId
        ArgumentCaptor<PostEntity> captor = ArgumentCaptor.forClass(PostEntity.class);
        when(postRepository.save(captor.capture())).thenAnswer(invocation -> {
            PostEntity entity = invocation.getArgument(0);
            // Simulate ID generation
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PostServiceImpl service = new PostServiceImpl(
                postRepository, postUpvoteRepository, reportRepository,
                imageStorageService, kafkaTemplate);

        CreatePostRequest request = new CreatePostRequest(PostType.TEXT, content, null);
        PostResponse response = service.createPost(userId, userClubId, request, null);

        // Property: post.clubId == author's favoriteClubId
        PostEntity savedPost = captor.getValue();
        assert savedPost.getClubId().equals(userClubId) :
                "Post clubId (" + savedPost.getClubId() + ") must equal user's favoriteClubId (" + userClubId + ")";
        assert response.clubId().equals(userClubId) :
                "Response clubId must equal user's favoriteClubId";
    }

    @Property(tries = 50)
    void createdPost_clubIdNeverNull_whenUserHasClub(
            @ForAll("randomUserId") UUID userId,
            @ForAll("randomClubId") UUID userClubId) {

        PostRepository postRepository = mock(PostRepository.class);
        PostUpvoteRepository postUpvoteRepository = mock(PostUpvoteRepository.class);
        ReportRepository reportRepository = mock(ReportRepository.class);
        ImageStorageService imageStorageService = mock(ImageStorageService.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

        ArgumentCaptor<PostEntity> captor = ArgumentCaptor.forClass(PostEntity.class);
        when(postRepository.save(captor.capture())).thenAnswer(invocation -> {
            PostEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        PostServiceImpl service = new PostServiceImpl(
                postRepository, postUpvoteRepository, reportRepository,
                imageStorageService, kafkaTemplate);

        CreatePostRequest request = new CreatePostRequest(PostType.MATCH_ANALYSIS, "Analysis of the game tactics and formation changes.", null);
        service.createPost(userId, userClubId, request, null);

        PostEntity savedPost = captor.getValue();
        assert savedPost.getClubId() != null : "Club ID must not be null when user has a favorite club";
        assert savedPost.getClubId().equals(userClubId) : "Club ID must match user's club";
    }

    // ---- Providers ----

    @Provide
    Arbitrary<UUID> randomUserId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<UUID> randomClubId() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<String> randomContent() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(2000).filter(s -> !s.isBlank());
    }
}
