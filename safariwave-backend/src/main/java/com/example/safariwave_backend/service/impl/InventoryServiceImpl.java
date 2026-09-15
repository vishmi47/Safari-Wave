package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.InventoryRequestDTO;
import com.example.safariwave_backend.dto.request.MaintenanceRecordDTO;
import com.example.safariwave_backend.dto.response.InventoryResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.InventoryItem;
import com.example.safariwave_backend.entity.MaintenanceRecord;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.InventoryRepository;
import com.example.safariwave_backend.repository.MaintenanceRecordRepository;
import com.example.safariwave_backend.service.InventoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final BoatRepository boatRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                MaintenanceRecordRepository maintenanceRecordRepository,
                                BoatRepository boatRepository) {
        this.inventoryRepository = inventoryRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.boatRepository = boatRepository;
    }

    @Override
    public List<InventoryResponseDTO> getAllItems() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public InventoryResponseDTO addInventoryItem(InventoryRequestDTO dto) {
        InventoryItem item = new InventoryItem();
        item.setItemName(dto.getItemName());
        item.setCategory(dto.getCategory());
        item.setQuantity(dto.getQuantity());
        item.setMinThreshold(dto.getMinThreshold());
        item.setUnitOfMeasure(dto.getUnitOfMeasure());
        item.setConditionStatus(dto.getConditionStatus() != null ? dto.getConditionStatus() : "GOOD");
        item.setLastInspectedDate(LocalDate.now());

        InventoryItem savedItem = inventoryRepository.save(item);
        return mapToInventoryResponse(savedItem);
    }

    @Override
    public InventoryResponseDTO updateStockQuantity(Long id, Integer newQuantity) {
        InventoryItem item = inventoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with ID: " + id));
        item.setQuantity(newQuantity);
        item.setLastInspectedDate(LocalDate.now());
        
        return mapToInventoryResponse(inventoryRepository.save(item));
    }

    @Override
    public List<InventoryResponseDTO> getLowStockAlerts() {
        return inventoryRepository.findLowStockItems().stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MaintenanceRecordDTO recordBoatMaintenance(MaintenanceRecordDTO dto) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setBoatId(dto.getBoatId());
        record.setMaintenanceDate(dto.getMaintenanceDate());
        record.setMaintenanceType(dto.getMaintenanceType());
        record.setDescription(dto.getDescription());
        record.setCost(dto.getCost());
        record.setTechnicianName(dto.getTechnicianName());
        record.setNextServiceDate(dto.getNextServiceDate());

        MaintenanceRecord savedRecord = maintenanceRecordRepository.save(record);

        Boat boat = boatRepository.findById(dto.getBoatId().intValue())
                .orElseThrow(() -> new RuntimeException("Boat not found with ID: " + dto.getBoatId()));
        
        if (dto.getBoatStatus() != null) {
            boat.setStatus(dto.getBoatStatus());
            boatRepository.save(boat);
        }

        dto.setId(savedRecord.getId());
        return dto;
    }

    @Override
    public List<MaintenanceRecordDTO> getMaintenanceHistoryByBoat(Long boatId) {
        return maintenanceRecordRepository.findByBoatId(boatId).stream().map(record -> {
            MaintenanceRecordDTO dto = new MaintenanceRecordDTO();
            dto.setId(record.getId());
            dto.setBoatId(record.getBoatId());
            dto.setMaintenanceDate(record.getMaintenanceDate());
            dto.setMaintenanceType(record.getMaintenanceType());
            dto.setDescription(record.getDescription());
            dto.setCost(record.getCost());
            dto.setTechnicianName(record.getTechnicianName());
            dto.setNextServiceDate(record.getNextServiceDate());
            return dto;
        }).collect(Collectors.toList());
    }

    private InventoryResponseDTO mapToInventoryResponse(InventoryItem item) {
        InventoryResponseDTO dto = new InventoryResponseDTO();
        dto.setId(item.getId());
        dto.setItemName(item.getItemName());
        dto.setCategory(item.getCategory());
        dto.setQuantity(item.getQuantity());
        dto.setMinThreshold(item.getMinThreshold());
        dto.setUnitOfMeasure(item.getUnitOfMeasure());
        dto.setConditionStatus(item.getConditionStatus());
        dto.setLastInspectedDate(item.getLastInspectedDate());
        dto.setLowStockWarning(item.getQuantity() <= item.getMinThreshold());
        return dto;
    }
}