package com.example.safariwave_backend.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BoatMaintenanceRequestDTO {

    private Integer boatId;
    private LocalDate inspectionDate;
    private String inspectionFindings;
    private String maintenanceDescription;
    private String maintenanceStatus;
    private BigDecimal maintenanceCost;
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
