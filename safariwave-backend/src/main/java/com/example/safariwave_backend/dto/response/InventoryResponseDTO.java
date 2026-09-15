package com.example.safariwave_backend.dto.response;

import lombok.Data;
import java.time.LocalDate;

@Data
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