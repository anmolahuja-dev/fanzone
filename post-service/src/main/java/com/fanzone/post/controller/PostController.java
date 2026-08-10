package com.fanzone.post.controller;

import com.fanzone.common.security.UserPrincipal;
import com.fanzone.post.dto.CreatePostRequest;
import com.fanzone.post.dto.PostResponse;
import com.fanzone.post.dto.ReportRequest;
import com.fanzone.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for post CRUD operations.
 * <p>
 * Supports:
 * - Creating posts (text, image, poll, match analysis) via multipart or JSON
 * - Getting a single post
 * - Deleting own posts
 * - Upvoting/removing upvotes (idempotent, self-upvote prevented)
 * - Reporting posts (one report per user per post)
 */
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Create a new post. Supports multipart (for image posts) and JSON-only (text/poll/analysis).
     * For image posts, send as multipart/form-data with "data" (JSON) and "image" (file) parts.
     * For non-image posts, send as application/json.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPostWithImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("data") @Valid CreatePostRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        principal.requireEmailVerified();

        PostResponse response = postService.createPost(
                principal.getUserId(),
                principal.getFavoriteClubId(),
                request,
                image
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a non-image post via JSON body (text, poll, match analysis).
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePostRequest request) {

        principal.requireEmailVerified();

        PostResponse response = postService.createPost(
                principal.getUserId(),
                principal.getFavoriteClubId(),
                request,
                null
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a single post by ID.
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable UUID postId) {
        PostResponse response = postService.getPost(postId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a post. Only the post author can delete their own posts.
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {

        postService.deletePost(postId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Upvote a post. Idempotent — repeated calls do not increment the count.
     * Self-upvote is prevented (returns 403).
     */
    @PostMapping("/{postId}/upvotes")
    public ResponseEntity<Void> upvote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {

        postService.upvote(postId, principal.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * Remove an upvote from a post. No-op if not previously upvoted.
     */
    @DeleteMapping("/{postId}/upvotes")
    public ResponseEntity<Void> removeUpvote(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId) {

        postService.removeUpvote(postId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Report a post. One report per user per post (returns 409 if already reported).
     */
    @PostMapping("/{postId}/reports")
    public ResponseEntity<Void> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID postId,
            @RequestBody(required = false) ReportRequest request) {

        String reason = request != null ? request.reason() : null;
        postService.report(postId, principal.getUserId(), reason);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
