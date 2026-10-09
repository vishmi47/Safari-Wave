package com.example.safariwave_backend.dto.response;

import lombok.Data;
import java.time.LocalDate;

/**
 * Legacy duplicate kept so existing teammate code keeps compiling. Not used by any endpoint.
 * @deprecated use AssetResponseDTO instead (canonical class for the Inventory & Asset module).
 */
@Data
@Deprecated
public class InventoryResponseDTO {
    private Long id;
    private String itemName;
    private String category;
    private Integer quantity;
    private Integer minThreshold;
    private String unitOfMeasure;
    private String conditionStatus;
    private LocalDate lastInspectedDate;
    private boolean lowStockWarning;
}