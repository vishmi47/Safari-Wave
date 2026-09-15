package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.AssetRequestDTO;
import com.example.safariwave_backend.dto.response.AssetResponseDTO;
import com.example.safariwave_backend.entity.Asset;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.repository.AssetRepository;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.service.AssetService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final BoatRepository boatRepository;

    public AssetServiceImpl(AssetRepository assetRepository, BoatRepository boatRepository) {
        this.assetRepository = assetRepository;
        this.boatRepository = boatRepository;
    }

    @Override
    public AssetResponseDTO createAsset(AssetRequestDTO dto) {
        if (dto.getAssetName() == null || dto.getAssetName().trim().isEmpty()) {
            throw new RuntimeException("Asset name is required.");
        }
        if (dto.getAssetType() == null || dto.getAssetType().trim().isEmpty()) {
            throw new RuntimeException("Asset type is required.");
        }

        Boat boat = null;
        if (dto.getBoatId() != null) {
            boat = boatRepository.findById(dto.getBoatId())
                    .orElseThrow(() -> new RuntimeException("Boat not found with id: " + dto.getBoatId()));
        }

        Asset asset = new Asset();
        asset.setAssetName(dto.getAssetName());
        asset.setAssetType(dto.getAssetType());
        asset.setQuantity(dto.getQuantity() == null ? 0 : dto.getQuantity());
        asset.setConditionStatus(dto.getConditionStatus() == null ? "GOOD" : dto.getConditionStatus().toUpperCase());
        asset.setStatus(dto.getStatus() == null ? "AVAILABLE" : dto.getStatus().toUpperCase());
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setMaintenanceStatus(dto.getMaintenanceStatus() == null ? "NOT_REQUIRED" : dto.getMaintenanceStatus().toUpperCase());
        asset.setDescription(dto.getDescription());
        asset.setBoat(boat);

        Asset saved = assetRepository.save(asset);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<AssetResponseDTO> getAssetById(Integer assetId) {
        return assetRepository.findById(assetId).map(this::toResponseDTO);
    }

    @Override
    public List<AssetResponseDTO> getAllAssets() {
        return assetRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<AssetResponseDTO> getAssetsByType(String assetType) {
        return assetRepository.findByAssetType(assetType).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<AssetResponseDTO> getAssetsByStatus(String status) {
        return assetRepository.findByStatus(status.toUpperCase()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public AssetResponseDTO updateAsset(Integer assetId, AssetRequestDTO dto) {
        Asset existing = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found with id: " + assetId));

        Boat boat = null;
        if (dto.getBoatId() != null) {
            boat = boatRepository.findById(dto.getBoatId())
                    .orElseThrow(() -> new RuntimeException("Boat not found with id: " + dto.getBoatId()));
        }

        existing.setAssetName(dto.getAssetName());
        existing.setAssetType(dto.getAssetType());
        existing.setQuantity(dto.getQuantity() == null ? 0 : dto.getQuantity());
        existing.setConditionStatus(dto.getConditionStatus() == null ? existing.getConditionStatus() : dto.getConditionStatus().toUpperCase());
        existing.setStatus(dto.getStatus() == null ? existing.getStatus() : dto.getStatus().toUpperCase());
        existing.setPurchaseDate(dto.getPurchaseDate());
        existing.setMaintenanceStatus(dto.getMaintenanceStatus() == null ? existing.getMaintenanceStatus() : dto.getMaintenanceStatus().toUpperCase());
        existing.setDescription(dto.getDescription());
        existing.setBoat(boat);

        Asset updated = assetRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteAsset(Integer assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new RuntimeException("Asset not found with id: " + assetId);
        }
        assetRepository.deleteById(assetId);
    }

    private AssetResponseDTO toResponseDTO(Asset asset) {
        AssetResponseDTO dto = new AssetResponseDTO();
        dto.setAssetId(asset.getAssetId());
        dto.setAssetName(asset.getAssetName());
        dto.setAssetType(asset.getAssetType());
        dto.setQuantity(asset.getQuantity());
        dto.setConditionStatus(asset.getConditionStatus());
        dto.setStatus(asset.getStatus());
        dto.setPurchaseDate(asset.getPurchaseDate());
        dto.setMaintenanceStatus(asset.getMaintenanceStatus());
        dto.setDescription(asset.getDescription());
        dto.setBoatId(asset.getBoat() != null ? asset.getBoat().getBoatId() : null);
        dto.setBoatName(asset.getBoat() != null ? asset.getBoat().getBoatName() : null);
        dto.setCreatedAt(asset.getCreatedAt());
        dto.setUpdatedAt(asset.getUpdatedAt());
        return dto;
    }
}
