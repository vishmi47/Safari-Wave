package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public class BoatMaintenanceRequestDTO {

    @NotNull
    private Integer boatId;
    @NotNull
    @PastOrPresent
    private LocalDate inspectionDate;
    @Size(max = 2000)
    private String inspectionFindings;
    @Size(max = 120)
    private String inspectedBy;
    @Size(max = 2000)
    private String maintenanceDescription;
    private String maintenanceStatus;
    @DecimalMin("0.00")
    private BigDecimal maintenanceCost;
    @Size(max = 1000)
    private String remarks;
    private LocalDate nextInspectionDate;

    public BoatMaintenanceRequestDTO() {
    }

    public Integer getBoatId() {
        return boatId;
    }

    public void setBoatId(Integer boatId) {
        this.boatId = boatId;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDate inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public String getInspectionFindings() {
        return inspectionFindings;
    }

    public void setInspectionFindings(String inspectionFindings) {
        this.inspectionFindings = inspectionFindings;
    }

    public String getInspectedBy() {
        return inspectedBy;
    }

    public void setInspectedBy(String inspectedBy) {
        this.inspectedBy = inspectedBy;
    }

    public String getMaintenanceDescription() {
        return maintenanceDescription;
    }

    public void setMaintenanceDescription(String maintenanceDescription) {
        this.maintenanceDescription = maintenanceDescription;
    }

    public String getMaintenanceStatus() {
        return maintenanceStatus;
    }

    public void setMaintenanceStatus(String maintenanceStatus) {
        this.maintenanceStatus = maintenanceStatus;
    }

    public BigDecimal getMaintenanceCost() {
        return maintenanceCost;
    }

    public void setMaintenanceCost(BigDecimal maintenanceCost) {
        this.maintenanceCost = maintenanceCost;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDate getNextInspectionDate() {
        return nextInspectionDate;
    }

    public void setNextInspectionDate(LocalDate nextInspectionDate) {
        this.nextInspectionDate = nextInspectionDate;
    }
}
