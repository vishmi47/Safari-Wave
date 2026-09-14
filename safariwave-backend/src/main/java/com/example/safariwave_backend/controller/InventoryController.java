package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.InventoryRequestDTO;
import com.example.safariwave_backend.dto.request.MaintenanceRecordDTO;
import com.example.safariwave_backend.dto.response.InventoryResponseDTO;
import com.example.safariwave_backend.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponseDTO>> getAllItems() {
        return ResponseEntity.ok(inventoryService.getAllItems());
    }

    @PostMapping
    public ResponseEntity<InventoryResponseDTO> addInventoryItem(@RequestBody InventoryRequestDTO requestDTO) {
        return ResponseEntity.ok(inventoryService.addInventoryItem(requestDTO));
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<InventoryResponseDTO> updateStockQuantity(
            @PathVariable Long id, 
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.updateStockQuantity(id, quantity));
    }

    @GetMapping("/alerts/low-stock")
    public ResponseEntity<List<InventoryResponseDTO>> getLowStockAlerts() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }

    @PostMapping("/maintenance")
    public ResponseEntity<MaintenanceRecordDTO> recordMaintenance(@RequestBody MaintenanceRecordDTO maintenanceDTO) {
        return ResponseEntity.ok(inventoryService.recordBoatMaintenance(maintenanceDTO));
    }

    @GetMapping("/maintenance/boat/{boatId}")
    public ResponseEntity<List<MaintenanceRecordDTO>> getBoatMaintenanceHistory(@PathVariable Long boatId) {
        return ResponseEntity.ok(inventoryService.getMaintenanceHistoryByBoat(boatId));
    }
}