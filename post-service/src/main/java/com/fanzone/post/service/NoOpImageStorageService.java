package com.fanzone.post.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Fallback image storage that returns a placeholder URL.
 * Used when S3 is not configured (local development without AWS).
 */
@Service
@ConditionalOnMissingBean(S3Client.class)
public class NoOpImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(NoOpImageStorageService.class);

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String placeholderUrl = "https://placeholder.fanzone.dev/" + folder + "/" + filename;
        log.warn("S3 not configured — returning placeholder URL: {}", placeholderUrl);
        return placeholderUrl;
    }
}
