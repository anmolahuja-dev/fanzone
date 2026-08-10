package com.fanzone.post.service;

import com.fanzone.common.events.CommentCreatedEvent;
import com.fanzone.common.events.CommentUpvotedEvent;
import com.fanzone.common.events.KafkaTopics;
import com.fanzone.common.exceptions.ConflictException;
import com.fanzone.common.exceptions.ForbiddenException;
import com.fanzone.common.exceptions.NotFoundException;
import com.fanzone.common.exceptions.ValidationException;
import com.fanzone.post.dto.CommentResponse;
import com.fanzone.post.model.CommentEntity;
import com.fanzone.post.model.CommentUpvoteEntity;
import com.fanzone.post.model.ReportEntity;
import com.fanzone.post.repository.CommentRepository;
import com.fanzone.post.repository.CommentUpvoteRepository;
import com.fanzone.post.repository.PostRepository;
import com.fanzone.post.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);
    private static final int MAX_NESTING_LEVEL = 3;
    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]+)");

    private final CommentRepository commentRepository;
    private final CommentUpvoteRepository commentUpvoteRepository;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CommentServiceImpl(CommentRepository commentRepository,
                              CommentUpvoteRepository commentUpvoteRepository,
                              PostRepository postRepository,
                              ReportRepository reportRepository,
                              KafkaTemplate<String, Object> kafkaTemplate) {
        this.commentRepository = commentRepository;
        this.commentUpvoteRepository = commentUpvoteRepository;
        this.postRepository = postRepository;
        this.reportRepository = reportRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CommentResponse createComment(UUID postId, UUID userId, String content) {
        validateContent(content);

        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("POST_NOT_FOUND", "Post not found");
        }

        CommentEntity comment = new CommentEntity();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setNestingLevel(0);

        CommentEntity saved = commentRepository.save(comment);

        // Increment comment count on post
        incrementPostCommentCount(postId);

        // Publish event with @mentions
        publishCommentCreatedEvent(saved, null);

        log.info("Created comment {} on post {} by user {}", saved.getId(), postId, userId);
        return toResponse(saved, List.of());
    }

    @Override
    public CommentResponse createReply(UUID parentCommentId, UUID userId, String content) {
        validateContent(content);

        CommentEntity parent = findCommentOrThrow(parentCommentId);

        // Enforce max nesting: if parent is at max level, redirect reply to that level
        int replyLevel = Math.min(parent.getNestingLevel() + 1, MAX_NESTING_LEVEL);

        // If parent is beyond max, attach to the max-level ancestor instead
        UUID effectiveParentId = parentCommentId;
        if (parent.getNestingLevel() >= MAX_NESTING_LEVEL) {
            // Reply becomes a sibling at level 3 rather than going deeper
            effectiveParentId = parentCommentId;
            replyLevel = MAX_NESTING_LEVEL;
        }

        CommentEntity reply = new CommentEntity();
        reply.setPostId(parent.getPostId());
        reply.setUserId(userId);
        reply.setContent(content);
        reply.setParentCommentId(effectiveParentId);
        reply.setNestingLevel(replyLevel);

        CommentEntity saved = commentRepository.save(reply);

        // Increment comment count on post
        incrementPostCommentCount(parent.getPostId());

        // Publish event — include parent author for reply notification
        publishCommentCreatedEvent(saved, parent.getUserId());

        log.info("Created reply {} to comment {} by user {} (level {})",
                saved.getId(), parentCommentId, userId, replyLevel);
        return toResponse(saved, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsForPost(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new NotFoundException("POST_NOT_FOUND", "Post not found");
        }

        // Fetch all comments for the post
        List<CommentEntity> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        // Build threaded tree
        return buildCommentTree(allComments);
    }

    @Override
    public void upvote(UUID commentId, UUID userId) {
        CommentEntity comment = findCommentOrThrow(commentId);

        // Prevent self-upvote
        if (comment.getUserId().equals(userId)) {
            throw new ForbiddenException("SELF_UPVOTE", "You cannot upvote your own comment");
        }

        // Idempotent — check if already upvoted
        if (commentUpvoteRepository.existsByUserIdAndCommentId(userId, commentId)) {
            return; // Already upvoted, no-op
        }

        CommentUpvoteEntity upvote = new CommentUpvoteEntity();
        upvote.setUserId(userId);
        upvote.setCommentId(commentId);
        commentUpvoteRepository.save(upvote);

        commentRepository.incrementUpvoteCount(commentId);

        // Publish event for reputation service
        kafkaTemplate.send(KafkaTopics.REPUTATION_EVENTS,
                commentId.toString(),
                new CommentUpvotedEvent(commentId, comment.getUserId(), userId, Instant.now()));

        log.debug("User {} upvoted comment {}", userId, commentId);
    }

    @Override
    public void removeUpvote(UUID commentId, UUID userId) {
        findCommentOrThrow(commentId); // Verify exists

        if (!commentUpvoteRepository.existsByUserIdAndCommentId(userId, commentId)) {
            return; // Not upvoted, no-op
        }

        commentUpvoteRepository.deleteByUserIdAndCommentId(userId, commentId);
        commentRepository.decrementUpvoteCount(commentId);

        log.debug("User {} removed upvote from comment {}", userId, commentId);
    }

    @Override
    public void report(UUID commentId, UUID reporterId, String reason) {
        findCommentOrThrow(commentId); // Verify exists

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, "comment", commentId)) {
            throw new ConflictException("ALREADY_REPORTED", "You have already reported this comment");
        }

        ReportEntity report = new ReportEntity();
        report.setReporterId(reporterId);
        report.setTargetType("comment");
        report.setTargetId(commentId);
        report.setReason(reason);
        reportRepository.save(report);

        log.info("User {} reported comment {} (reason: {})", reporterId, commentId, reason);
    }

    // ---- Pure logic methods exposed for property testing ----

    /**
     * Computes the effective nesting level for a reply given the parent's level.
     * Nesting never exceeds MAX_NESTING_LEVEL (3).
     */
    public static int computeReplyNestingLevel(int parentLevel) {
        return Math.min(parentLevel + 1, MAX_NESTING_LEVEL);
    }

    /**
     * Returns the maximum allowed nesting level.
     */
    public static int getMaxNestingLevel() {
        return MAX_NESTING_LEVEL;
    }

    // ---- Private helpers ----

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ValidationException("COMMENT_CONTENT_REQUIRED", "Comment content is required");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ValidationException("COMMENT_CONTENT_TOO_LONG",
                    "Comment must not exceed " + MAX_CONTENT_LENGTH + " characters");
        }
    }

    private CommentEntity findCommentOrThrow(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("COMMENT_NOT_FOUND", "Comment not found"));
    }

    private void incrementPostCommentCount(UUID postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setCommentCount(post.getCommentCount() + 1);
            postRepository.save(post);
        });
    }

    private void publishCommentCreatedEvent(CommentEntity comment, UUID parentAuthorId) {
        try {
            List<String> mentions = extractMentions(comment.getContent());
            String preview = comment.getContent().length() > 100
                    ? comment.getContent().substring(0, 100)
                    : comment.getContent();

            CommentCreatedEvent event = new CommentCreatedEvent(
                    comment.getId(),
                    comment.getPostId(),
                    comment.getUserId(),
                    parentAuthorId,
                    mentions,
                    preview,
                    Instant.now()
            );
            kafkaTemplate.send(KafkaTopics.NOTIFICATION_EVENTS, comment.getPostId().toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish CommentCreatedEvent for comment {}: {}",
                    comment.getId(), e.getMessage());
        }
    }

    /**
     * Extracts @mentions from comment content.
     */
    static List<String> extractMentions(String content) {
        if (content == null) return List.of();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        List<String> mentions = new ArrayList<>();
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }

    /**
     * Builds a threaded comment tree from a flat list.
     */
    private List<CommentResponse> buildCommentTree(List<CommentEntity> allComments) {
        Map<UUID, List<CommentEntity>> childrenMap = allComments.stream()
                .filter(c -> c.getParentCommentId() != null)
                .collect(Collectors.groupingBy(CommentEntity::getParentCommentId));

        return allComments.stream()
                .filter(c -> c.getParentCommentId() == null)
                .map(c -> buildTreeNode(c, childrenMap))
                .collect(Collectors.toList());
    }

    private CommentResponse buildTreeNode(CommentEntity comment, Map<UUID, List<CommentEntity>> childrenMap) {
        List<CommentEntity> children = childrenMap.getOrDefault(comment.getId(), List.of());
        List<CommentResponse> replies = children.stream()
                .map(c -> buildTreeNode(c, childrenMap))
                .collect(Collectors.toList());
        return toResponse(comment, replies);
    }

    private CommentResponse toResponse(CommentEntity comment, List<CommentResponse> replies) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getUserId(),
                comment.getParentCommentId(),
                comment.getContent(),
                comment.getUpvoteCount(),
                comment.getNestingLevel(),
                comment.getCreatedAt(),
                replies.isEmpty() ? null : replies
        );
    }
}
