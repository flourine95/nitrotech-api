package com.nitrotech.api.application.upload.controller;

import com.nitrotech.api.application.upload.request.PresignRequest;
import com.nitrotech.api.infrastructure.storage.CloudinaryStorageService;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final CloudinaryStorageService storageService;

    public UploadController(CloudinaryStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/sign")
    public ResponseEntity<ApiResponse<CloudinaryStorageService.SignatureResult>> sign(
            @Valid @RequestBody PresignRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(storageService.generateSignature(req.folder())));
    }

    @GetMapping("/folders")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> folders(
            @RequestParam(required = false) String parent
    ) {
        return ResponseEntity.ok(ApiResponse.ok(storageService.listFolders(parent)));
    }

    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<CloudinaryStorageService.AssetsResult>> assets(
            @RequestParam(defaultValue = "uploads") String folder,
            @RequestParam(defaultValue = "50") int maxResults,
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(storageService.listAssets(folder, maxResults, cursor)));
    }
}
