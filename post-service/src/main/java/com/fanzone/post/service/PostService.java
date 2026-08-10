package com.fanzone.post.service;

import com.fanzone.post.dto.CreatePostRequest;
import com.fanzone.post.dto.PostResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PostService {

    PostResponse createPost(UUID userId, UUID userClubId, CreatePostRequest request, MultipartFile image);

    PostResponse getPost(UUID postId);

    void deletePost(UUID postId, UUID userId);

    void upvote(UUID postId, UUID userId);

    void removeUpvote(UUID postId, UUID userId);

    void report(UUID postId, UUID reporterId, String reason);
}
