package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.BoatMaintenanceRequestDTO;
import com.example.safariwave_backend.dto.response.BoatMaintenanceResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.BoatMaintenanceRecord;
import com.example.safariwave_backend.repository.BoatMaintenanceRecordRepository;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.service.BoatMaintenanceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BoatMaintenanceServiceImpl implements BoatMaintenanceService {

    private final BoatMaintenanceRecordRepository maintenanceRepository;
    private final BoatRepository boatRepository;

    public BoatMaintenanceServiceImpl(BoatMaintenanceRecordRepository maintenanceRepository, BoatRepository boatRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.boatRepository = boatRepository;
    }

    @Override
    public BoatMaintenanceResponseDTO createMaintenanceRecord(BoatMaintenanceRequestDTO dto) {
        if (dto.getBoatId() == null) {
            throw new RuntimeException("Boat is required.");
        }
        Boat boat = boatRepository.findById(dto.getBoatId())
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + dto.getBoatId()));

        BoatMaintenanceRecord record = new BoatMaintenanceRecord();
        record.setBoat(boat);
        record.setInspectionDate(dto.getInspectionDate());
        record.setInspectionFindings(dto.getInspectionFindings());
        record.setMaintenanceDescription(dto.getMaintenanceDescription());
        record.setMaintenanceStatus(dto.getMaintenanceStatus() == null ? "OPEN" : dto.getMaintenanceStatus().toUpperCase());
        record.setMaintenanceCost(dto.getMaintenanceCost());
        record.setRemarks(dto.getRemarks());
        record.setNextInspectionDate(dto.getNextInspectionDate());

        if ("UNDER_MAINTENANCE".equalsIgnoreCase(record.getMaintenanceStatus()) || "UNSAFE".equalsIgnoreCase(record.getMaintenanceStatus())) {
            boat.setStatus("UNDER_MAINTENANCE");
            boatRepository.save(boat);
        }

        if ("COMPLETED".equalsIgnoreCase(record.getMaintenanceStatus()) || "OPERATIONAL".equalsIgnoreCase(record.getMaintenanceStatus())) {
            boat.setStatus("OPERATIONAL");
            boatRepository.save(boat);
        }

        BoatMaintenanceRecord saved = maintenanceRepository.save(record);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<BoatMaintenanceResponseDTO> getMaintenanceRecordById(Integer maintenanceId) {
        return maintenanceRepository.findById(maintenanceId).map(this::toResponseDTO);
    }

    @Override
    public List<BoatMaintenanceResponseDTO> getAllMaintenanceRecords() {
        return maintenanceRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<BoatMaintenanceResponseDTO> getMaintenanceRecordsByBoat(Integer boatId) {
        Boat boat = boatRepository.findById(boatId)
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + boatId));
        return maintenanceRepository.findByBoat(boat).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public BoatMaintenanceResponseDTO updateMaintenanceRecord(Integer maintenanceId, BoatMaintenanceRequestDTO dto) {
        BoatMaintenanceRecord existing = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance record not found with id: " + maintenanceId));

        Boat boat = boatRepository.findById(dto.getBoatId())
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + dto.getBoatId()));

        existing.setBoat(boat);
        existing.setInspectionDate(dto.getInspectionDate());
        existing.setInspectionFindings(dto.getInspectionFindings());
        existing.setMaintenanceDescription(dto.getMaintenanceDescription());
        existing.setMaintenanceStatus(dto.getMaintenanceStatus() == null ? existing.getMaintenanceStatus() : dto.getMaintenanceStatus().toUpperCase());
        existing.setMaintenanceCost(dto.getMaintenanceCost());
        existing.setRemarks(dto.getRemarks());
        existing.setNextInspectionDate(dto.getNextInspectionDate());

        if ("UNDER_MAINTENANCE".equalsIgnoreCase(existing.getMaintenanceStatus()) || "UNSAFE".equalsIgnoreCase(existing.getMaintenanceStatus())) {
            boat.setStatus("UNDER_MAINTENANCE");
            boatRepository.save(boat);
        }

        if ("COMPLETED".equalsIgnoreCase(existing.getMaintenanceStatus()) || "OPERATIONAL".equalsIgnoreCase(existing.getMaintenanceStatus())) {
            boat.setStatus("OPERATIONAL");
            boatRepository.save(boat);
        }

        return toResponseDTO(maintenanceRepository.save(existing));
    }

    @Override
    public void deleteMaintenanceRecord(Integer maintenanceId) {
        if (!maintenanceRepository.existsById(maintenanceId)) {
            throw new RuntimeException("Maintenance record not found with id: " + maintenanceId);
        }
        maintenanceRepository.deleteById(maintenanceId);
    }

    private BoatMaintenanceResponseDTO toResponseDTO(BoatMaintenanceRecord record) {
        BoatMaintenanceResponseDTO dto = new BoatMaintenanceResponseDTO();
        dto.setMaintenanceId(record.getMaintenanceId());
        dto.setBoatId(record.getBoat() != null ? record.getBoat().getBoatId() : null);
        dto.setBoatName(record.getBoat() != null ? record.getBoat().getBoatName() : null);
        dto.setInspectionDate(record.getInspectionDate());
        dto.setInspectionFindings(record.getInspectionFindings());
        dto.setMaintenanceDescription(record.getMaintenanceDescription());
        dto.setMaintenanceStatus(record.getMaintenanceStatus());
        dto.setMaintenanceCost(record.getMaintenanceCost());
        dto.setRemarks(record.getRemarks());
        dto.setNextInspectionDate(record.getNextInspectionDate());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }
}
