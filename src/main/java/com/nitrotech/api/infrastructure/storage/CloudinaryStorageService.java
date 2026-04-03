package com.nitrotech.api.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CloudinaryStorageService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public SignatureResult generateSignature(String folder) {
        long timestamp = System.currentTimeMillis() / 1000;

        Map<String, Object> params = ObjectUtils.asMap(
                "folder", folder,
                "timestamp", timestamp
        );

        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);

        return new SignatureResult(
                signature,
                String.valueOf(timestamp),
                cloudinary.config.apiKey,
                cloudName,
                folder
        );
    }

    public String buildUrl(String publicId, String transformation) {
        return cloudinary.url()
                .transformation(new com.cloudinary.Transformation().rawTransformation(transformation))
                .generate(publicId);
    }

    public record SignatureResult(
            String signature,
            String timestamp,
            String apiKey,
            String cloudName,
            String folder
    ) {}
}
