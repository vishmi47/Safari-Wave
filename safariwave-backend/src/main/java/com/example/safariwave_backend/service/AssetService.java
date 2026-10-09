package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.AssetRequestDTO;
import com.example.safariwave_backend.dto.response.AssetResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AssetService {

    AssetResponseDTO createAsset(AssetRequestDTO dto);

    Optional<AssetResponseDTO> getAssetById(Integer assetId);

    List<AssetResponseDTO> getAllAssets();

    List<AssetResponseDTO> getAssetsByType(String assetType);

    List<AssetResponseDTO> getAssetsByStatus(String status);

    Page<AssetResponseDTO> searchAssets(String query, String status, int page, int size);

    AssetResponseDTO updateAsset(Integer assetId, AssetRequestDTO dto);

    /** Guides (and operations) report an asset's condition, e.g. a damaged life jacket. */
    AssetResponseDTO updateCondition(Integer assetId, String conditionStatus, String remarks);

    /** Retires the asset (status RETIRED). With permanent=true an ADMIN removes it completely. */
    void deleteAsset(Integer assetId, boolean permanent);
}
