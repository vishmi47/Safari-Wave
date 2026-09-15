package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.InventoryRequestDTO;
import com.example.safariwave_backend.dto.request.MaintenanceRecordDTO;
import com.example.safariwave_backend.dto.response.InventoryResponseDTO;

import java.util.List;

public interface InventoryService {
    List<InventoryResponseDTO> getAllItems();
    InventoryResponseDTO addInventoryItem(InventoryRequestDTO requestDTO);
    InventoryResponseDTO updateStockQuantity(Long id, Integer newQuantity);
    List<InventoryResponseDTO> getLowStockAlerts();
    MaintenanceRecordDTO recordBoatMaintenance(MaintenanceRecordDTO maintenanceDTO);
    List<MaintenanceRecordDTO> getMaintenanceHistoryByBoat(Long boatId);
}