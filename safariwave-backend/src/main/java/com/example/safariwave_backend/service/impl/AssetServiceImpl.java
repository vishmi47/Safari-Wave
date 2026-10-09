package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.AssetRequestDTO;
import com.example.safariwave_backend.dto.response.AssetResponseDTO;
import com.example.safariwave_backend.entity.Asset;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.AssetRepository;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.AssetService;
import com.example.safariwave_backend.service.NotificationService;
import com.example.safariwave_backend.service.support.PagingSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Operational assets (life jackets, paddles, engines, GPS, first-aid kits...) - Inventory & Asset module (spec 6.2 / B.3).
 *
 * OPERATIONS: create, read, update, retire. GUIDE: read and report condition. FINANCE: read.
 * CUSTOMER: no access. ADMIN: everything, including permanent delete.
 */
@Service
public class AssetServiceImpl implements AssetService {

    static final List<String> ASSET_STATUSES = List.of("AVAILABLE", "IN_USE", "UNDER_MAINTENANCE", "RETIRED");
    static final List<String> CONDITIONS = List.of("GOOD", "FAIR", "POOR", "DAMAGED");
    static final List<String> MAINTENANCE_STATUSES = List.of("NOT_REQUIRED", "SCHEDULED", "IN_PROGRESS", "COMPLETED");
    private static final String[] READ_ROLES = {Roles.OPERATIONS, Roles.GUIDE, Roles.FINANCE};

    private final AssetRepository assetRepository;
    private final BoatRepository boatRepository;
    private final NotificationService notificationService;

    public AssetServiceImpl(AssetRepository assetRepository, BoatRepository boatRepository, NotificationService notificationService) {
        this.assetRepository = assetRepository;
        this.boatRepository = boatRepository;
        this.notificationService = notificationService;
    }

    @Override
    public AssetResponseDTO createAsset(AssetRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        Asset asset = new Asset();
        copyRequestToEntity(dto, asset, "GOOD", "AVAILABLE", "NOT_REQUIRED");
        asset.setCreatedBy(CurrentUser.id());

        Asset saved = assetRepository.save(asset);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<AssetResponseDTO> getAssetById(Integer assetId) {
        CurrentUser.requireAnyRole(READ_ROLES);
        return assetRepository.findById(assetId).map(this::toResponseDTO);
    }

    @Override
    public List<AssetResponseDTO> getAllAssets() {
        CurrentUser.requireAnyRole(READ_ROLES);
        return assetRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<AssetResponseDTO> getAssetsByType(String assetType) {
        CurrentUser.requireAnyRole(READ_ROLES);
        return assetRepository.findByAssetType(assetType).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<AssetResponseDTO> getAssetsByStatus(String status) {
        CurrentUser.requireAnyRole(READ_ROLES);
        return assetRepository.findByStatus(status.toUpperCase()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public Page<AssetResponseDTO> searchAssets(String query, String status, int page, int size) {
        CurrentUser.requireAnyRole(READ_ROLES);
        return assetRepository.search(PagingSupport.text(query), PagingSupport.status(status, ASSET_STATUSES),
                        PagingSupport.pageRequest(page, size, Sort.by("assetName")))
                .map(this::toResponseDTO);
    }

    @Override
    public AssetResponseDTO updateAsset(Integer assetId, AssetRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        Asset existing = findAsset(assetId);
        copyRequestToEntity(dto, existing, existing.getConditionStatus(), existing.getStatus(), existing.getMaintenanceStatus());

        Asset updated = assetRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public AssetResponseDTO updateCondition(Integer assetId, String conditionStatus, String remarks) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS, Roles.GUIDE);
        Asset asset = findAsset(assetId);
        String condition = requireOneOf(conditionStatus, CONDITIONS, "Condition");
        asset.setConditionStatus(condition);
        Asset updated = assetRepository.save(asset);

        // Let the operations team know when a guide flags equipment as unsafe to use
        if (CurrentUser.hasAnyRole(Roles.GUIDE) && List.of("POOR", "DAMAGED").contains(condition)) {
            String note = remarks == null || remarks.isBlank() ? "" : " Note: " + remarks.trim();
            notificationService.notifyRole(Roles.OPERATIONS, "ASSET_CONDITION",
                    asset.getAssetName() + " was reported " + condition.toLowerCase(Locale.ROOT) + " by a guide." + note);
        }
        return toResponseDTO(updated);
    }

    @Override
    public void deleteAsset(Integer assetId, boolean permanent) {
        Asset asset = findAsset(assetId);
        if (permanent) {
            CurrentUser.requireAnyRole(); // ADMIN only
            assetRepository.delete(asset);
            return;
        }
        // Spec B.3: operations "delete" retires the asset so its history is kept
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        asset.setStatus("RETIRED");
        assetRepository.save(asset);
    }

    private void copyRequestToEntity(AssetRequestDTO dto, Asset asset, String defaultCondition, String defaultStatus, String defaultMaintenance) {
        Boat boat = null;
        if (dto.getBoatId() != null) {
            boat = boatRepository.findById(dto.getBoatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Boat not found with id: " + dto.getBoatId()));
        }
        asset.setAssetName(dto.getAssetName().trim());
        asset.setAssetType(dto.getAssetType().trim());
        asset.setQuantity(dto.getQuantity());
        asset.setConditionStatus(dto.getConditionStatus() == null ? defaultCondition : requireOneOf(dto.getConditionStatus(), CONDITIONS, "Condition"));
        asset.setStatus(dto.getStatus() == null ? defaultStatus : requireOneOf(dto.getStatus(), ASSET_STATUSES, "Status"));
        asset.setPurchaseDate(dto.getPurchaseDate());
        asset.setMaintenanceStatus(dto.getMaintenanceStatus() == null ? defaultMaintenance
                : requireOneOf(dto.getMaintenanceStatus(), MAINTENANCE_STATUSES, "Maintenance status"));
        asset.setDescription(dto.getDescription());
        asset.setBoat(boat);
    }

    private String requireOneOf(String value, List<String> allowed, String label) {
        String normalized = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || !allowed.contains(normalized)) {
            throw new BadRequestException(label + " must be one of " + allowed + ".");
        }
        return normalized;
    }

    private Asset findAsset(Integer assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + assetId));
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
        dto.setCreatedBy(asset.getCreatedBy());
        return dto;
    }
}
