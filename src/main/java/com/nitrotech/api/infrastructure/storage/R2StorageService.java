package com.nitrotech.api.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class R2StorageService {

    private final S3Presigner presigner;

    @Value("${r2.bucket}")
    private String bucket;

    @Value("${r2.public-url}")
    private String publicUrl;

    public R2StorageService(S3Presigner presigner) {
        this.presigner = presigner;
    }

    public PresignResult generatePresignedUrl(String folder, String filename, String contentType) {
        String key = buildKey(folder, filename);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(5))
                        .putObjectRequest(putRequest)
                        .build()
        );

        String filePublicUrl = publicUrl.replaceAll("/+$", "") + "/" + key;
        return new PresignResult(presigned.url().toString(), filePublicUrl, key);
    }

    private String buildKey(String folder, String filename) {
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
        return folder.replaceAll("/+$", "") + "/" + UUID.randomUUID() + ext;
    }

    public record PresignResult(String uploadUrl, String publicUrl, String key) {}
}
