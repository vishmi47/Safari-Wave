package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.EmergencyIncidentRequestDTO;
import com.example.safariwave_backend.dto.response.EmergencyIncidentResponseDTO;

import java.util.List;
import java.util.Optional;

public interface EmergencyIncidentService {

    EmergencyIncidentResponseDTO createEmergencyIncident(EmergencyIncidentRequestDTO dto);

    Optional<EmergencyIncidentResponseDTO> getEmergencyIncidentById(Integer incidentId);

    List<EmergencyIncidentResponseDTO> getAllEmergencyIncidents();

    List<EmergencyIncidentResponseDTO> getEmergencyIncidentsBySeverity(String severity);

    List<EmergencyIncidentResponseDTO> getEmergencyIncidentsByStatus(String status);

    EmergencyIncidentResponseDTO updateEmergencyIncident(Integer incidentId, EmergencyIncidentRequestDTO dto);

    void deleteEmergencyIncident(Integer incidentId);
}
