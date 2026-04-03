package com.nitrotech.api.infrastructure.storage;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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

        Map<String, Object> params = new HashMap<>();
        params.put("folder", folder);
        params.put("timestamp", timestamp);

        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret, 1);

        return new SignatureResult(
                signature,
                String.valueOf(timestamp),
                cloudinary.config.apiKey,
                cloudName,
                folder
        );
    }

    public record SignatureResult(
            String signature,
            String timestamp,
            String apiKey,
            String cloudName,
            String folder
    ) {}
}
