package com.fanzone.post.controller;

import com.fanzone.common.security.UserPrincipal;
import com.fanzone.post.dto.CommentResponse;
import com.fanzone.post.dto.CreateCommentRequest;
import com.fanzone.post.dto.ReportRequest;
import com.fanzone.post.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for comments and replies.
 * <p>
 * Supports threaded comments with max 3 levels of nesting.
 * Upvotes are idempotent, self-upvote is forbidden (403).
 * Reports enforce one-per-user-per-comment uniqueness.
 */
@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Get threaded comments for a post.
     * Returns top-level comments with nested replies (up to 3 levels deep).
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID postId) {
        List<CommentResponse> comments = commentService.getCommentsForPost(postId);
        return ResponseEntity.ok(comments);
    }

    /**
     * Create a top-level comment on a post.
     * Requires email verification.
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {

        principal.requireEmailVerified();

        CommentResponse response = commentService.createComment(
                postId, principal.getUserId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Reply to an existing comment.
     * Nesting is capped at 3 levels — deeper replies are redirected to level 3.
     * Requires email verification.
     */
    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentResponse> createReply(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateCommentRequest request) {

        principal.requireEmailVerified();

        CommentResponse response = commentService.createReply(
                commentId, principal.getUserId(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Upvote a comment. Idempotent — repeated calls are no-ops.
     * Self-upvote returns 403.
     */
    @PostMapping("/comments/{commentId}/upvotes")
    public ResponseEntity<Void> upvote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID commentId) {

        commentService.upvote(commentId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * Remove an upvote from a comment. No-op if not previously upvoted.
     */
    @DeleteMapping("/comments/{commentId}/upvotes")
    public ResponseEntity<Void> removeUpvote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID commentId) {

        commentService.removeUpvote(commentId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Report a comment. One report per user per comment (returns 409 if already reported).
     */
    @PostMapping("/comments/{commentId}/reports")
    public ResponseEntity<Void> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID commentId,
            @RequestBody(required = false) ReportRequest request) {

        String reason = request != null ? request.reason() : null;
        commentService.report(commentId, principal.getUserId(), reason);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
