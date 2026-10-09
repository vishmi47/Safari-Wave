package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.SafetyInspectionRequestDTO;
import com.example.safariwave_backend.dto.response.SafetyInspectionResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.entity.SafetyInspection;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.SafetyInspectionRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.NotificationService;
import com.example.safariwave_backend.service.SafetyInspectionService;
import com.example.safariwave_backend.service.support.PagingSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Safety inspections - Weather, Safety & Emergency module (spec 6.4 / B.3).
 * GUIDE and OPERATIONS record inspections; a guide edits only their own; OPERATIONS edits all and deletes
 * invalid or duplicate records. A FAILED inspection notifies the operations team.
 */
@Service
public class SafetyInspectionServiceImpl implements SafetyInspectionService {

    static final List<String> SAFETY_STATUSES = List.of("PASSED", "NEEDS_ATTENTION", "FAILED");

    private final SafetyInspectionRepository safetyInspectionRepository;
    private final BoatRepository boatRepository;
    private final SafariTripRepository safariTripRepository;
    private final NotificationService notificationService;

    public SafetyInspectionServiceImpl(SafetyInspectionRepository safetyInspectionRepository,
                                      BoatRepository boatRepository,
                                      SafariTripRepository safariTripRepository,
                                      NotificationService notificationService) {
        this.safetyInspectionRepository = safetyInspectionRepository;
        this.boatRepository = boatRepository;
        this.safariTripRepository = safariTripRepository;
        this.notificationService = notificationService;
    }

    @Override
    public SafetyInspectionResponseDTO createSafetyInspection(SafetyInspectionRequestDTO dto) {
        SafetyPermissions.requireCanRecord();
        SafetyInspection inspection = new SafetyInspection();
        copyRequestToEntity(dto, inspection, "PASSED");
        if (inspection.getInspector() == null || inspection.getInspector().isBlank()) {
            inspection.setInspector(CurrentUser.get().getName());
        }
        inspection.setCreatedBy(CurrentUser.id());

        SafetyInspection saved = safetyInspectionRepository.save(inspection);
        notifyIfFailed(saved);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<SafetyInspectionResponseDTO> getSafetyInspectionById(Integer inspectionId) {
        SafetyPermissions.requireStaffRead();
        return safetyInspectionRepository.findById(inspectionId).map(this::toResponseDTO);
    }

    @Override
    public List<SafetyInspectionResponseDTO> getAllSafetyInspections() {
        SafetyPermissions.requireStaffRead();
        return safetyInspectionRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<SafetyInspectionResponseDTO> getSafetyInspectionsByBoat(Integer boatId) {
        SafetyPermissions.requireStaffRead();
        Boat boat = boatRepository.findById(boatId)
                .orElseThrow(() -> new ResourceNotFoundException("Boat not found with id: " + boatId));
        return safetyInspectionRepository.findByBoat(boat).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public Page<SafetyInspectionResponseDTO> searchSafetyInspections(String query, String status, int page, int size) {
        SafetyPermissions.requireStaffRead();
        return safetyInspectionRepository.search(PagingSupport.text(query), PagingSupport.status(status, SAFETY_STATUSES),
                        PagingSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "inspectionDate")))
                .map(this::toResponseDTO);
    }

    @Override
    public SafetyInspectionResponseDTO updateSafetyInspection(Integer inspectionId, SafetyInspectionRequestDTO dto) {
        SafetyInspection existing = findInspection(inspectionId);
        SafetyPermissions.requireCanEdit(existing.getCreatedBy());
        String previousStatus = existing.getSafetyStatus();
        copyRequestToEntity(dto, existing, existing.getSafetyStatus());

        SafetyInspection saved = safetyInspectionRepository.save(existing);
        if (!"FAILED".equals(previousStatus)) {
            notifyIfFailed(saved);
        }
        return toResponseDTO(saved);
    }

    @Override
    public void deleteSafetyInspection(Integer inspectionId) {
        SafetyPermissions.requireCanDelete();
        safetyInspectionRepository.delete(findInspection(inspectionId));
    }

    private void copyRequestToEntity(SafetyInspectionRequestDTO dto, SafetyInspection inspection, String defaultStatus) {
        if (dto.getBoatId() == null && dto.getTripId() == null) {
            throw new BadRequestException("Choose the boat or trip that was inspected.");
        }
        Boat boat = null;
        if (dto.getBoatId() != null) {
            boat = boatRepository.findById(dto.getBoatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Boat not found with id: " + dto.getBoatId()));
        }
        SafariTrip safariTrip = null;
        if (dto.getTripId() != null) {
            safariTrip = safariTripRepository.findById(dto.getTripId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + dto.getTripId()));
            if (boat == null) {
                boat = safariTrip.getBoat();
            }
        }
        inspection.setBoat(boat);
        inspection.setSafariTrip(safariTrip);
        inspection.setInspectionDate(dto.getInspectionDate());
        inspection.setInspector(dto.getInspector());
        inspection.setFindings(dto.getFindings());
        inspection.setSafetyStatus(SafetyPermissions.oneOf(dto.getSafetyStatus(), SAFETY_STATUSES, "Safety status", defaultStatus));
        inspection.setRemarks(dto.getRemarks());
    }

    private void notifyIfFailed(SafetyInspection inspection) {
        if ("FAILED".equals(inspection.getSafetyStatus())) {
            String subject = inspection.getBoat() != null ? inspection.getBoat().getBoatName() : "a trip";
            notificationService.notifyRole(Roles.OPERATIONS, "SAFETY_INSPECTION_FAILED",
                    "Safety inspection failed for " + subject + " on " + inspection.getInspectionDate() + ". Review before the next trip.");
        }
    }

    private SafetyInspection findInspection(Integer inspectionId) {
        return safetyInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection not found with id: " + inspectionId));
    }

    private SafetyInspectionResponseDTO toResponseDTO(SafetyInspection inspection) {
        SafetyInspectionResponseDTO dto = new SafetyInspectionResponseDTO();
        dto.setInspectionId(inspection.getInspectionId());
        dto.setBoatId(inspection.getBoat() != null ? inspection.getBoat().getBoatId() : null);
        dto.setBoatName(inspection.getBoat() != null ? inspection.getBoat().getBoatName() : null);
        dto.setTripId(inspection.getSafariTrip() != null ? inspection.getSafariTrip().getTripId() : null);
        dto.setTripSummary(inspection.getSafariTrip() != null ? inspection.getSafariTrip().getBoat().getBoatName() + " - " + inspection.getSafariTrip().getTripDate() : null);
        dto.setInspectionDate(inspection.getInspectionDate());
        dto.setInspector(inspection.getInspector());
        dto.setFindings(inspection.getFindings());
        dto.setSafetyStatus(inspection.getSafetyStatus());
        dto.setRemarks(inspection.getRemarks());
        dto.setCreatedAt(inspection.getCreatedAt());
        dto.setUpdatedAt(inspection.getUpdatedAt());
        dto.setCreatedBy(inspection.getCreatedBy());
        return dto;
    }
}
