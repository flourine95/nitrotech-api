package com.nitrotech.api.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
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

        return new SignatureResult(signature, String.valueOf(timestamp),
                cloudinary.config.apiKey, cloudName, folder);
    }

    @SuppressWarnings("unchecked")
    public AssetsResult listAssets(String folder, int maxResults, String nextCursor) {
        try {
            Map<String, Object> options = new HashMap<>();
            options.put("type", "upload");
            options.put("prefix", folder);
            options.put("max_results", Math.min(maxResults, 100));
            options.put("resource_type", "image");
            if (nextCursor != null && !nextCursor.isBlank()) {
                options.put("next_cursor", nextCursor);
            }
            ApiResponse response = cloudinary.api().resources(options);
            List<Map<String, Object>> resources = (List<Map<String, Object>>) response.get("resources");
            String cursor = (String) response.get("next_cursor");
            return new AssetsResult(resources, cursor);
        } catch (Exception e) {
            throw new RuntimeException("Failed to list Cloudinary assets: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listFolders(String parentFolder) {
        try {
            ApiResponse response = parentFolder == null || parentFolder.isBlank()
                    ? cloudinary.api().rootFolders(new HashMap<>())
                    : cloudinary.api().subFolders(parentFolder, new HashMap<>());
            return (List<Map<String, Object>>) response.get("folders");
        } catch (Exception e) {
            throw new RuntimeException("Failed to list Cloudinary folders: " + e.getMessage(), e);
        }
    }

    public record SignatureResult(String signature, String timestamp,
                                   String apiKey, String cloudName, String folder) {}

    public record AssetsResult(List<Map<String, Object>> resources, String nextCursor) {}
}
