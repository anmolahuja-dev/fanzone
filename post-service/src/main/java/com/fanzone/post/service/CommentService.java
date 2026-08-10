package com.fanzone.post.service;

import com.fanzone.post.dto.CommentResponse;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentResponse createComment(UUID postId, UUID userId, String content);

    CommentResponse createReply(UUID parentCommentId, UUID userId, String content);

    List<CommentResponse> getCommentsForPost(UUID postId);

    void upvote(UUID commentId, UUID userId);

    void removeUpvote(UUID commentId, UUID userId);

    void report(UUID commentId, UUID reporterId, String reason);
}
