package com.nitrotech.api.application.upload.controller;

import com.nitrotech.api.application.upload.request.PresignRequest;
import com.nitrotech.api.infrastructure.storage.CloudinaryStorageService;
import com.nitrotech.api.shared.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "Upload", description = "Cloudinary file upload and asset management APIs")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryStorageService storageService;

    @Operation(summary = "Generate upload signature", description = "Generate a signed upload signature for direct client-side upload to Cloudinary. The client uses this signature to upload files without exposing API secrets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Signature generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/sign")
    public ResponseEntity<ApiResult<CloudinaryStorageService.SignatureResult>> sign(
            @Valid @RequestBody PresignRequest req
    ) {
        return ResponseEntity.ok(ApiResult.ok(storageService.generateSignature(req.folder())));
    }

    @Operation(summary = "List folders", description = "List Cloudinary folders, optionally under a parent folder.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/folders")
    public ResponseEntity<ApiResult<List<Map<String, Object>>>> folders(
            @Parameter(description = "Parent folder path (omit for root)") @RequestParam(required = false) String parent
    ) {
        return ResponseEntity.ok(ApiResult.ok(storageService.listFolders(parent)));
    }

    @Operation(summary = "List assets", description = "List assets in a Cloudinary folder with pagination support.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Assets retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/assets")
    public ResponseEntity<ApiResult<CloudinaryStorageService.AssetsResult>> assets(
            @Parameter(description = "Folder path to list assets from") @RequestParam(required = false) String folder,
            @Parameter(description = "Maximum number of results to return") @RequestParam(defaultValue = "50") int maxResults,
            @Parameter(description = "Pagination cursor from previous response") @RequestParam(required = false) String cursor,
            @Parameter(description = "Filter assets created after this date (ISO 8601)") @RequestParam(required = false) String startAt
    ) {
        return ResponseEntity.ok(ApiResult.ok(storageService.listAssets(folder, maxResults, cursor, startAt)));
    }
}
