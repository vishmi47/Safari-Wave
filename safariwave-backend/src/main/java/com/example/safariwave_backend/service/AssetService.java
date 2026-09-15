package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.AssetRequestDTO;
import com.example.safariwave_backend.dto.response.AssetResponseDTO;

import java.util.List;
import java.util.Optional;

public interface AssetService {

    AssetResponseDTO createAsset(AssetRequestDTO dto);

    Optional<AssetResponseDTO> getAssetById(Integer assetId);

    List<AssetResponseDTO> getAllAssets();

    List<AssetResponseDTO> getAssetsByType(String assetType);

    List<AssetResponseDTO> getAssetsByStatus(String status);

    AssetResponseDTO updateAsset(Integer assetId, AssetRequestDTO dto);

    void deleteAsset(Integer assetId);
}
