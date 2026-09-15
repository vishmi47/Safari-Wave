package com.example.safariwave_backend.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MaintenanceRecordDTO {
    private Long id;
    private Long boatId;
    private LocalDate maintenanceDate;
    private String maintenanceType;
    private String description;
    private Double cost;
    private String technicianName;
    private LocalDate nextServiceDate;
    private String boatStatus;
}