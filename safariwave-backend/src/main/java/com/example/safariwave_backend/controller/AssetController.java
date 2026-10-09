package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.AssetRequestDTO;
import com.example.safariwave_backend.dto.response.AssetResponseDTO;
import com.example.safariwave_backend.service.AssetService;
import org.springframework.data.web.PagedModel;
import com.example.safariwave_backend.dto.request.StatusUpdateRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

// Role rules for assets are enforced in AssetServiceImpl.
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public ResponseEntity<AssetResponseDTO> createAsset(@Valid @RequestBody AssetRequestDTO dto) {
        AssetResponseDTO saved = assetService.createAsset(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AssetResponseDTO>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    // Paged list with search: GET /api/assets/search?q=&status=&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<PagedModel<AssetResponseDTO>> searchAssets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(assetService.searchAssets(q, status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDTO> getAssetById(@PathVariable Integer id) {
        return assetService.getAssetById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{assetType}")
    public ResponseEntity<List<AssetResponseDTO>> getAssetsByType(@PathVariable String assetType) {
        return ResponseEntity.ok(assetService.getAssetsByType(assetType));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AssetResponseDTO>> getAssetsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(assetService.getAssetsByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponseDTO> updateAsset(@PathVariable Integer id, @Valid @RequestBody AssetRequestDTO dto) {
        AssetResponseDTO updated = assetService.updateAsset(id, dto);
        return ResponseEntity.ok(updated);
    }

    // Guides and operations report an asset's condition
    @PatchMapping("/{id}/condition")
    public ResponseEntity<AssetResponseDTO> updateCondition(@PathVariable Integer id, @Valid @RequestBody StatusUpdateRequestDTO dto) {
        return ResponseEntity.ok(assetService.updateCondition(id, dto.getStatus(), dto.getRemarks()));
    }

    // Retires the asset; ADMIN may pass ?permanent=true to remove it completely
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean permanent) {
        assetService.deleteAsset(id, permanent);
        return ResponseEntity.noContent().build();
    }
}
