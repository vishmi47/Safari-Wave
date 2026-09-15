package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.SafetyInspectionRequestDTO;
import com.example.safariwave_backend.dto.response.SafetyInspectionResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.entity.SafetyInspection;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.SafetyInspectionRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.service.SafetyInspectionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SafetyInspectionServiceImpl implements SafetyInspectionService {

    private final SafetyInspectionRepository safetyInspectionRepository;
    private final BoatRepository boatRepository;
    private final SafariTripRepository safariTripRepository;

    public SafetyInspectionServiceImpl(SafetyInspectionRepository safetyInspectionRepository,
                                      BoatRepository boatRepository,
                                      SafariTripRepository safariTripRepository) {
        this.safetyInspectionRepository = safetyInspectionRepository;
        this.boatRepository = boatRepository;
        this.safariTripRepository = safariTripRepository;
    }

    @Override
    public SafetyInspectionResponseDTO createSafetyInspection(SafetyInspectionRequestDTO dto) {
        if (dto == null || dto.getInspectionDate() == null) {
            throw new RuntimeException("Inspection date is required.");
        }

        Boat boat = null;
        if (dto.getBoatId() != null) {
            boat = boatRepository.findById(dto.getBoatId())
                    .orElseThrow(() -> new RuntimeException("Boat not found with id: " + dto.getBoatId()));
        }

        SafariTrip safariTrip = null;
        if (dto.getTripId() != null) {
            safariTrip = safariTripRepository.findById(dto.getTripId())
                    .orElseThrow(() -> new RuntimeException("Trip not found with id: " + dto.getTripId()));
        }

        SafetyInspection inspection = new SafetyInspection();
        inspection.setBoat(boat);
        inspection.setSafariTrip(safariTrip);
        inspection.setInspectionDate(dto.getInspectionDate());
        inspection.setInspector(dto.getInspector());
        inspection.setFindings(dto.getFindings());
        inspection.setSafetyStatus(dto.getSafetyStatus() == null ? "PASSED" : dto.getSafetyStatus().toUpperCase());
        inspection.setRemarks(dto.getRemarks());

        SafetyInspection saved = safetyInspectionRepository.save(inspection);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<SafetyInspectionResponseDTO> getSafetyInspectionById(Integer inspectionId) {
        return safetyInspectionRepository.findById(inspectionId).map(this::toResponseDTO);
    }

    @Override
    public List<SafetyInspectionResponseDTO> getAllSafetyInspections() {
        return safetyInspectionRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<SafetyInspectionResponseDTO> getSafetyInspectionsByBoat(Integer boatId) {
        Boat boat = boatRepository.findById(boatId)
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + boatId));
        return safetyInspectionRepository.findByBoat(boat).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public SafetyInspectionResponseDTO updateSafetyInspection(Integer inspectionId, SafetyInspectionRequestDTO dto) {
        SafetyInspection existing = safetyInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection not found with id: " + inspectionId));

        Boat boat = null;
        if (dto.getBoatId() != null) {
            boat = boatRepository.findById(dto.getBoatId())
                    .orElseThrow(() -> new RuntimeException("Boat not found with id: " + dto.getBoatId()));
        }

        SafariTrip safariTrip = null;
        if (dto.getTripId() != null) {
            safariTrip = safariTripRepository.findById(dto.getTripId())
                    .orElseThrow(() -> new RuntimeException("Trip not found with id: " + dto.getTripId()));
        }

        existing.setBoat(boat);
        existing.setSafariTrip(safariTrip);
        existing.setInspectionDate(dto.getInspectionDate());
        existing.setInspector(dto.getInspector());
        existing.setFindings(dto.getFindings());
        existing.setSafetyStatus(dto.getSafetyStatus() == null ? existing.getSafetyStatus() : dto.getSafetyStatus().toUpperCase());
        existing.setRemarks(dto.getRemarks());

        return toResponseDTO(safetyInspectionRepository.save(existing));
    }

    @Override
    public void deleteSafetyInspection(Integer inspectionId) {
        if (!safetyInspectionRepository.existsById(inspectionId)) {
            throw new RuntimeException("Inspection not found with id: " + inspectionId);
        }
        safetyInspectionRepository.deleteById(inspectionId);
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
        return dto;
    }
}
