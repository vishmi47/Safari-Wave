package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.InventoryRequestDTO;
import com.example.safariwave_backend.dto.request.MaintenanceRecordDTO;
import com.example.safariwave_backend.dto.response.InventoryResponseDTO;

import java.util.List;

/**
 * Legacy duplicate kept so existing teammate code keeps compiling. Not used by any endpoint.
 * @deprecated use AssetService instead (canonical class for the Inventory & Asset module).
 */
@Deprecated
public interface InventoryService {
    List<InventoryResponseDTO> getAllItems();
    InventoryResponseDTO addInventoryItem(InventoryRequestDTO requestDTO);
    InventoryResponseDTO updateStockQuantity(Long id, Integer newQuantity);
    List<InventoryResponseDTO> getLowStockAlerts();
    MaintenanceRecordDTO recordBoatMaintenance(MaintenanceRecordDTO maintenanceDTO);
    List<MaintenanceRecordDTO> getMaintenanceHistoryByBoat(Long boatId);
}