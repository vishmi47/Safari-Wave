package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.SafetyInspectionRequestDTO;
import com.example.safariwave_backend.dto.response.SafetyInspectionResponseDTO;

import java.util.List;
import java.util.Optional;

public interface SafetyInspectionService {

    SafetyInspectionResponseDTO createSafetyInspection(SafetyInspectionRequestDTO dto);

    Optional<SafetyInspectionResponseDTO> getSafetyInspectionById(Integer inspectionId);

    List<SafetyInspectionResponseDTO> getAllSafetyInspections();

    List<SafetyInspectionResponseDTO> getSafetyInspectionsByBoat(Integer boatId);

    SafetyInspectionResponseDTO updateSafetyInspection(Integer inspectionId, SafetyInspectionRequestDTO dto);

    void deleteSafetyInspection(Integer inspectionId);
}
