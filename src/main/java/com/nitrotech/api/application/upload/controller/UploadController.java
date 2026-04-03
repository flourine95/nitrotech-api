package com.nitrotech.api.application.upload.controller;

import com.nitrotech.api.application.upload.request.PresignRequest;
import com.nitrotech.api.infrastructure.storage.CloudinaryStorageService;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final CloudinaryStorageService storageService;

    public UploadController(CloudinaryStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Trả về signature để client upload trực tiếp lên Cloudinary.
     *
     * Client dùng response này để POST lên:
     * https://api.cloudinary.com/v1_1/<cloudName>/image/upload
     *
     * Form fields cần gửi:
     * - file: <file binary>
     * - api_key, timestamp, signature, folder (từ response này)
     */
    @PostMapping("/sign")
    public ResponseEntity<ApiResponse<CloudinaryStorageService.SignatureResult>> sign(
            @Valid @RequestBody PresignRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(storageService.generateSignature(req.folder())));
    }
}
