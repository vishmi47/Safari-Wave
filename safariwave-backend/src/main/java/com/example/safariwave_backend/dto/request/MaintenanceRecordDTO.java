package com.example.safariwave_backend.dto.request;

import lombok.Data;
import java.time.LocalDate;

/**
 * Legacy duplicate kept so existing teammate code keeps compiling. Not used by any endpoint.
 * @deprecated use BoatMaintenanceRequestDTO instead (canonical class for the Inventory & Asset module).
 */
@Data
@Deprecated
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