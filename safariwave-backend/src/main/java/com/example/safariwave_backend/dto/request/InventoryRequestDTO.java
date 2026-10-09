package com.example.safariwave_backend.dto.request;

import lombok.Data;

/**
 * Legacy duplicate kept so existing teammate code keeps compiling. Not used by any endpoint.
 * @deprecated use AssetRequestDTO instead (canonical class for the Inventory & Asset module).
 */
@Data
@Deprecated
public class InventoryRequestDTO {
    private String itemName;
    private String category;
    private Integer quantity;
    private Integer minThreshold;
    private String unitOfMeasure;
    private String conditionStatus;
}