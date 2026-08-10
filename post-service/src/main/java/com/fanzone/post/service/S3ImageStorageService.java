package com.fanzone.post.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * S3-backed image storage service.
 * Only activated when S3Client bean is available (i.e., fanzone.s3.bucket-name is configured).
 */
@Service
@ConditionalOnBean(S3Client.class)
public class S3ImageStorageService implements ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3ImageStorageService.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String cdnBaseUrl;

    public S3ImageStorageService(
            S3Client s3Client,
            @Value("${fanzone.s3.bucket-name}") String bucketName,
            @Value("${fanzone.s3.cdn-base-url:}") String cdnBaseUrl) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.cdnBaseUrl = cdnBaseUrl;
    }

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        String extension = getExtension(file.getContentType());
        String key = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

            String url = cdnBaseUrl.isBlank()
                    ? String.format("https://%s.s3.amazonaws.com/%s", bucketName, key)
                    : cdnBaseUrl + "/" + key;

            log.info("Uploaded image to S3: {}", key);
            return url;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to S3", e);
        }
    }

    private String getExtension(String contentType) {
        if (contentType == null) return "";
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
