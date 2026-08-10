package com.fanzone.post.service;

import com.fanzone.common.enums.PostType;
import com.fanzone.common.events.FeedInvalidationEvent;
import com.fanzone.common.events.KafkaTopics;
import com.fanzone.common.events.PostUpvotedEvent;
import com.fanzone.common.exceptions.ConflictException;
import com.fanzone.common.exceptions.ForbiddenException;
import com.fanzone.common.exceptions.NotFoundException;
import com.fanzone.post.dto.CreatePostRequest;
import com.fanzone.post.dto.PollOptionResponse;
import com.fanzone.post.dto.PostResponse;
import com.fanzone.post.model.PollOptionEntity;
import com.fanzone.post.model.PostEntity;
import com.fanzone.post.model.PostUpvoteEntity;
import com.fanzone.post.model.ReportEntity;
import com.fanzone.post.repository.PostRepository;
import com.fanzone.post.repository.PostUpvoteRepository;
import com.fanzone.post.repository.ReportRepository;
import com.fanzone.post.validation.PostValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);

    private final PostRepository postRepository;
    private final PostUpvoteRepository postUpvoteRepository;
    private final ReportRepository reportRepository;
    private final ImageStorageService imageStorageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PostServiceImpl(PostRepository postRepository,
                           PostUpvoteRepository postUpvoteRepository,
                           ReportRepository reportRepository,
                           ImageStorageService imageStorageService,
                           KafkaTemplate<String, Object> kafkaTemplate) {
        this.postRepository = postRepository;
        this.postUpvoteRepository = postUpvoteRepository;
        this.reportRepository = reportRepository;
        this.imageStorageService = imageStorageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public PostResponse createPost(UUID userId, UUID userClubId, CreatePostRequest request, MultipartFile image) {
        PostType postType = request.postType();

        // Validate content based on post type
        PostValidator.validate(postType, request.content(), request.pollOptions(), image);

        // Create entity
        PostEntity post = new PostEntity();
        post.setUserId(userId);
        post.setClubId(userClubId);
        post.setPostType(postType.name().toLowerCase());
        post.setContent(request.content());

        // Handle image upload
        if (image != null && !image.isEmpty()) {
            String imageUrl = imageStorageService.uploadImage(image, "posts");
            post.setImageUrl(imageUrl);
        }

        // Handle poll options
        if (postType == PostType.POLL && request.pollOptions() != null) {
            for (int i = 0; i < request.pollOptions().size(); i++) {
                PollOptionEntity option = new PollOptionEntity();
                option.setPost(post);
                option.setOptionText(request.pollOptions().get(i));
                option.setDisplayOrder(i);
                post.getPollOptions().add(option);
            }
        }

        PostEntity saved = postRepository.save(post);
        log.info("Created post {} (type={}) by user {}", saved.getId(), postType, userId);

        // Publish feed invalidation event
        publishFeedInvalidation(saved);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId) {
        PostEntity post = findPostOrThrow(postId);
        return toResponse(post);
    }

    @Override
    public void deletePost(UUID postId, UUID userId) {
        PostEntity post = findPostOrThrow(postId);

        if (!post.getUserId().equals(userId)) {
            throw new ForbiddenException("POST_NOT_OWNER", "You can only delete your own posts");
        }

        postRepository.delete(post);
        log.info("Deleted post {} by user {}", postId, userId);
    }

    @Override
    public void upvote(UUID postId, UUID userId) {
        PostEntity post = findPostOrThrow(postId);

        // Prevent self-upvote
        if (post.getUserId().equals(userId)) {
            throw new ForbiddenException("SELF_UPVOTE", "You cannot upvote your own post");
        }

        // Idempotent — check if already upvoted
        if (postUpvoteRepository.existsByUserIdAndPostId(userId, postId)) {
            return; // Already upvoted, no-op
        }

        PostUpvoteEntity upvote = new PostUpvoteEntity();
        upvote.setUserId(userId);
        upvote.setPostId(postId);
        postUpvoteRepository.save(upvote);

        postRepository.incrementUpvoteCount(postId);

        // Publish event for reputation service
        kafkaTemplate.send(KafkaTopics.REPUTATION_EVENTS,
                postId.toString(),
                new PostUpvotedEvent(postId, post.getUserId(), userId, Instant.now()));

        log.debug("User {} upvoted post {}", userId, postId);
    }

    @Override
    public void removeUpvote(UUID postId, UUID userId) {
        findPostOrThrow(postId); // Verify post exists

        if (!postUpvoteRepository.existsByUserIdAndPostId(userId, postId)) {
            return; // Not upvoted, no-op
        }

        postUpvoteRepository.deleteByUserIdAndPostId(userId, postId);
        postRepository.decrementUpvoteCount(postId);

        log.debug("User {} removed upvote from post {}", userId, postId);
    }

    @Override
    public void report(UUID postId, UUID reporterId, String reason) {
        findPostOrThrow(postId); // Verify post exists

        // Idempotent — one report per user per post
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, "post", postId)) {
            throw new ConflictException("ALREADY_REPORTED", "You have already reported this post");
        }

        ReportEntity report = new ReportEntity();
        report.setReporterId(reporterId);
        report.setTargetType("post");
        report.setTargetId(postId);
        report.setReason(reason);
        reportRepository.save(report);

        postRepository.incrementToxicReportCount(postId);

        log.info("User {} reported post {} (reason: {})", reporterId, postId, reason);
    }

    // ---- Helpers ----

    private PostEntity findPostOrThrow(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Post not found"));
    }

    private void publishFeedInvalidation(PostEntity post) {
        try {
            FeedInvalidationEvent event = new FeedInvalidationEvent(
                    post.getClubId(),
                    post.getId(),
                    "new_post",
                    Instant.now()
            );
            kafkaTemplate.send(KafkaTopics.FEED_INVALIDATION, post.getClubId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish feed invalidation event for post {}: {}", post.getId(), e.getMessage());
        }
    }

    private PostResponse toResponse(PostEntity post) {
        List<PollOptionResponse> options = post.getPollOptions().stream()
                .map(opt -> new PollOptionResponse(opt.getId(), opt.getOptionText(), opt.getVoteCount(), opt.getDisplayOrder()))
                .toList();

        return new PostResponse(
                post.getId(),
                post.getUserId(),
                post.getClubId(),
                PostType.valueOf(post.getPostType().toUpperCase()),
                post.getContent(),
                post.getImageUrl(),
                options.isEmpty() ? null : options,
                post.getCommentCount(),
                post.getUpvoteCount(),
                post.isFlagged(),
                post.getCreatedAt()
        );
    }
}
