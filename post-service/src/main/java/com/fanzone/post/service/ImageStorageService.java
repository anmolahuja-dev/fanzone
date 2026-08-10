package com.fanzone.post.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction for image storage. Backed by S3 in production,
 * can be swapped with a local/mock implementation for tests.
 */
public interface ImageStorageService {

    /**
     * Uploads an image and returns its public URL.
     *
     * @param file   the image file
     * @param folder the folder/prefix to store under (e.g., "posts", "avatars")
     * @return the publicly accessible URL of the uploaded image
     */
    String uploadImage(MultipartFile file, String folder);
}
