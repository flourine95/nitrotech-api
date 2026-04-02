package com.nitrotech.api.application.upload.controller;

import com.nitrotech.api.application.upload.request.PresignRequest;
import com.nitrotech.api.infrastructure.storage.R2StorageService;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final R2StorageService storageService;

    public UploadController(R2StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/presign")
    public ResponseEntity<ApiResponse<Map<String, String>>> presign(
            @Valid @RequestBody PresignRequest req
    ) {
        String folder = req.folder() != null ? req.folder() : "uploads";
        R2StorageService.PresignResult result = storageService.generatePresignedUrl(
                folder, req.filename(), req.contentType());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "uploadUrl", result.uploadUrl(),
                "publicUrl", result.publicUrl(),
                "key", result.key()
        )));
    }
}
