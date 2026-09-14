package com.example.safariwave_backend.dto.request;

import lombok.Data;

@Data
public class InventoryRequestDTO {
    private String itemName;
    private String category;
    private Integer quantity;
    private Integer minThreshold;
    private String unitOfMeasure;
    private String conditionStatus;
}