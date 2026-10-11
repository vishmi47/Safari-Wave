package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.BoatMaintenanceRequestDTO;
import com.example.safariwave_backend.dto.request.IssueReportRequestDTO;
import com.example.safariwave_backend.dto.response.BoatMaintenanceResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.BoatMaintenanceRecord;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.BoatMaintenanceRecordRepository;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.BoatMaintenanceService;
import com.example.safariwave_backend.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Boat maintenance records - Inventory & Asset module (spec 6.2 / B.3).
 *
 * OPERATIONS: create, read, update. GUIDE: read and report issues. FINANCE: read (costs).
 * CUSTOMER: no access. Deleting maintenance history is ADMIN only.
 * Record status drives boat availability: UNDER_MAINTENANCE / UNSAFE take the boat out of service, COMPLETED returns it.
 */
@Service
public class BoatMaintenanceServiceImpl implements BoatMaintenanceService {

    static final List<String> RECORD_STATUSES = List.of("OPEN", "UNDER_MAINTENANCE", "UNSAFE", "COMPLETED");
    private static final String[] READ_ROLES = {Roles.OPERATIONS, Roles.GUIDE, Roles.FINANCE};

    private final BoatMaintenanceRecordRepository maintenanceRepository;
    private final BoatRepository boatRepository;
    private final NotificationService notificationService;

    public BoatMaintenanceServiceImpl(BoatMaintenanceRecordRepository maintenanceRepository, BoatRepository boatRepository,
                                      NotificationService notificationService) {
        this.maintenanceRepository = maintenanceRepository;
        this.boatRepository = boatRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public BoatMaintenanceResponseDTO createMaintenanceRecord(BoatMaintenanceRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        Boat boat = findBoat(dto.getBoatId());

        BoatMaintenanceRecord record = new BoatMaintenanceRecord();
        copyRequestToEntity(dto, record, boat);
        if (record.getInspectedBy() == null || record.getInspectedBy().isBlank()) {
            record.setInspectedBy(CurrentUser.get().getName());
        }
        record.setCreatedBy(CurrentUser.id());
        syncBoatStatus(record);

        BoatMaintenanceRecord saved = maintenanceRepository.save(record);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<BoatMaintenanceResponseDTO> getMaintenanceRecordById(Integer maintenanceId) {
        CurrentUser.requireAnyRole(READ_ROLES);
        return maintenanceRepository.findById(maintenanceId).map(this::toResponseDTO);
    }

    @Override
    public List<BoatMaintenanceResponseDTO> getAllMaintenanceRecords() {
        CurrentUser.requireAnyRole(READ_ROLES);
        return maintenanceRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<BoatMaintenanceResponseDTO> getMaintenanceRecordsByBoat(Integer boatId) {
        CurrentUser.requireAnyRole(READ_ROLES);
        Boat boat = findBoat(boatId);
        return maintenanceRepository.findByBoat(boat).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BoatMaintenanceResponseDTO updateMaintenanceRecord(Integer maintenanceId, BoatMaintenanceRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        BoatMaintenanceRecord existing = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId));
        Boat boat = findBoat(dto.getBoatId());

        copyRequestToEntity(dto, existing, boat);
        syncBoatStatus(existing);

        return toResponseDTO(maintenanceRepository.save(existing));
    }

    @Override
    @Transactional
    public BoatMaintenanceResponseDTO reportIssue(IssueReportRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.GUIDE, Roles.OPERATIONS);
        Boat boat = findBoat(dto.getBoatId());

        BoatMaintenanceRecord record = new BoatMaintenanceRecord();
        record.setBoat(boat);
        record.setInspectionDate(LocalDate.now());
        record.setInspectionFindings(dto.getDescription().trim());
        record.setInspectedBy(CurrentUser.get().getName());
        record.setMaintenanceStatus("OPEN");
        record.setRemarks("Reported by " + CurrentUser.get().getName());
        record.setCreatedBy(CurrentUser.id());
        BoatMaintenanceRecord saved = maintenanceRepository.save(record);

        notificationService.notifyRole(Roles.OPERATIONS, "BOAT_ISSUE",
                "Issue reported on " + boat.getBoatName() + ": " + dto.getDescription().trim());
        return toResponseDTO(saved);
    }

    @Override
    public void deleteMaintenanceRecord(Integer maintenanceId) {
        CurrentUser.requireAnyRole(); // ADMIN only: maintenance history is kept
        if (!maintenanceRepository.existsById(maintenanceId)) {
            throw new ResourceNotFoundException("Maintenance record not found with id: " + maintenanceId);
        }
        maintenanceRepository.deleteById(maintenanceId);
    }

    private void copyRequestToEntity(BoatMaintenanceRequestDTO dto, BoatMaintenanceRecord record, Boat boat) {
        String status = dto.getMaintenanceStatus() == null ? "OPEN" : dto.getMaintenanceStatus().trim().toUpperCase(Locale.ROOT);
        if (!RECORD_STATUSES.contains(status)) {
            throw new BadRequestException("Maintenance status must be one of " + RECORD_STATUSES + ".");
        }
        if (dto.getNextInspectionDate() != null && dto.getInspectionDate() != null
                && !dto.getNextInspectionDate().isAfter(dto.getInspectionDate())) {
            throw new BadRequestException("Next inspection date must be after the inspection date.");
        }
        record.setBoat(boat);
        record.setInspectionDate(dto.getInspectionDate());
        record.setInspectionFindings(dto.getInspectionFindings());
        record.setInspectedBy(dto.getInspectedBy() == null ? record.getInspectedBy() : dto.getInspectedBy().trim());
        record.setMaintenanceDescription(dto.getMaintenanceDescription());
        record.setMaintenanceStatus(status);
        record.setMaintenanceCost(dto.getMaintenanceCost());
        record.setRemarks(dto.getRemarks());
        record.setNextInspectionDate(dto.getNextInspectionDate());
    }

    /** Takes the boat out of service while it is being repaired and returns it when the work is completed. */
    private void syncBoatStatus(BoatMaintenanceRecord record) {
        Boat boat = record.getBoat();
        if ("RETIRED".equalsIgnoreCase(boat.getStatus())) {
            return; // a retired boat stays retired
        }
        if ("UNDER_MAINTENANCE".equals(record.getMaintenanceStatus()) || "UNSAFE".equals(record.getMaintenanceStatus())) {
            boat.setStatus("UNDER_MAINTENANCE");
            boatRepository.save(boat);
        } else if ("COMPLETED".equals(record.getMaintenanceStatus())) {
            boat.setStatus("OPERATIONAL");
            boatRepository.save(boat);
        }
    }

    private Boat findBoat(Integer boatId) {
        if (boatId == null) {
            throw new BadRequestException("Boat is required.");
        }
        return boatRepository.findById(boatId)
                .orElseThrow(() -> new ResourceNotFoundException("Boat not found with id: " + boatId));
    }

    private BoatMaintenanceResponseDTO toResponseDTO(BoatMaintenanceRecord record) {
        BoatMaintenanceResponseDTO dto = new BoatMaintenanceResponseDTO();
        dto.setMaintenanceId(record.getMaintenanceId());
        dto.setBoatId(record.getBoat() != null ? record.getBoat().getBoatId() : null);
        dto.setBoatName(record.getBoat() != null ? record.getBoat().getBoatName() : null);
        dto.setInspectionDate(record.getInspectionDate());
        dto.setInspectionFindings(record.getInspectionFindings());
        dto.setInspectedBy(record.getInspectedBy());
        dto.setMaintenanceDescription(record.getMaintenanceDescription());
        dto.setMaintenanceStatus(record.getMaintenanceStatus());
        dto.setMaintenanceCost(record.getMaintenanceCost());
        dto.setRemarks(record.getRemarks());
        dto.setNextInspectionDate(record.getNextInspectionDate());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        dto.setCreatedBy(record.getCreatedBy());
        return dto;
    }
}
