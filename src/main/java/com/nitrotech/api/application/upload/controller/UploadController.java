package com.nitrotech.api.application.upload.controller;

import com.nitrotech.api.application.upload.request.PresignRequest;
import com.nitrotech.api.infrastructure.storage.CloudinaryStorageService;
import com.nitrotech.api.shared.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryStorageService storageService;

    @PostMapping("/sign")
    public ResponseEntity<ApiResult<CloudinaryStorageService.SignatureResult>> sign(
            @Valid @RequestBody PresignRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(storageService.generateSignature(req.folder())));
    }

    @GetMapping("/folders")
    public ResponseEntity<ApiResult<List<Map<String, Object>>>> folders(
            @RequestParam(required = false) String parent
    ) {
        return ResponseEntity.ok(ApiResult.ok(storageService.listFolders(parent)));
    }

    @GetMapping("/assets")
    public ResponseEntity<ApiResult<CloudinaryStorageService.AssetsResult>> assets(
            @RequestParam(required = false) String folder,
            @RequestParam(defaultValue = "50") int maxResults,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String startAt
    ) {
        return ResponseEntity.ok(ApiResult.ok(storageService.listAssets(folder, maxResults, cursor, startAt)));
    }
}