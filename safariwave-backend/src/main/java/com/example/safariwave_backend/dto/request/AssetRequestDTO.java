package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;

public class AssetRequestDTO {

    @NotBlank
    @Size(max = 120)
    private String assetName;
    @NotBlank
    @Size(max = 60)
    private String assetType;
    @NotNull
    @Min(0)
    private Integer quantity;
    private String conditionStatus;
    private String status;
    @PastOrPresent
    private LocalDate purchaseDate;
    private String maintenanceStatus;
    @Size(max = 2000)
    private String description;
    private Integer boatId;
    private List<@Min(1) Integer> boatIds;

    public AssetRequestDTO() {
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getConditionStatus() {
        return conditionStatus;
    }

    public void setConditionStatus(String conditionStatus) {
        this.conditionStatus = conditionStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getMaintenanceStatus() {
        return maintenanceStatus;
    }

    public void setMaintenanceStatus(String maintenanceStatus) {
        this.maintenanceStatus = maintenanceStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getBoatId() {
        return boatId;
    }

    public void setBoatId(Integer boatId) {
        this.boatId = boatId;
    }

    public List<Integer> getBoatIds() {
        return boatIds;
    }

    public void setBoatIds(List<Integer> boatIds) {
        this.boatIds = boatIds;
    }
}
