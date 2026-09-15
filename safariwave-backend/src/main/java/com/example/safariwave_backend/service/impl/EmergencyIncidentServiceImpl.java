package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.EmergencyIncidentRequestDTO;
import com.example.safariwave_backend.dto.response.EmergencyIncidentResponseDTO;
import com.example.safariwave_backend.entity.EmergencyIncident;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.repository.EmergencyIncidentRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.service.EmergencyIncidentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmergencyIncidentServiceImpl implements EmergencyIncidentService {

    private final EmergencyIncidentRepository incidentRepository;
    private final SafariTripRepository safariTripRepository;

    public EmergencyIncidentServiceImpl(EmergencyIncidentRepository incidentRepository, SafariTripRepository safariTripRepository) {
        this.incidentRepository = incidentRepository;
        this.safariTripRepository = safariTripRepository;
    }

    @Override
    public EmergencyIncidentResponseDTO createEmergencyIncident(EmergencyIncidentRequestDTO dto) {
        if (dto == null || dto.getIncidentDateTime() == null) {
            throw new RuntimeException("Incident date and time are required.");
        }

        SafariTrip safariTrip = null;
        if (dto.getTripId() != null) {
            safariTrip = safariTripRepository.findById(dto.getTripId())
                    .orElseThrow(() -> new RuntimeException("Trip not found with id: " + dto.getTripId()));
        }

        EmergencyIncident incident = new EmergencyIncident();
        incident.setSafariTrip(safariTrip);
        incident.setIncidentDateTime(dto.getIncidentDateTime());
        incident.setIncidentType(dto.getIncidentType());
        incident.setSeverity(dto.getSeverity());
        incident.setDescription(dto.getDescription());
        incident.setResponseAction(dto.getResponseAction());
        incident.setStatus(dto.getStatus() == null ? "OPEN" : dto.getStatus().toUpperCase());
        incident.setRemarks(dto.getRemarks());

        EmergencyIncident saved = incidentRepository.save(incident);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<EmergencyIncidentResponseDTO> getEmergencyIncidentById(Integer incidentId) {
        return incidentRepository.findById(incidentId).map(this::toResponseDTO);
    }

    @Override
    public List<EmergencyIncidentResponseDTO> getAllEmergencyIncidents() {
        return incidentRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<EmergencyIncidentResponseDTO> getEmergencyIncidentsBySeverity(String severity) {
        return incidentRepository.findBySeverity(severity).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<EmergencyIncidentResponseDTO> getEmergencyIncidentsByStatus(String status) {
        return incidentRepository.findByStatus(status.toUpperCase()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public EmergencyIncidentResponseDTO updateEmergencyIncident(Integer incidentId, EmergencyIncidentRequestDTO dto) {
        EmergencyIncident existing = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Emergency incident not found with id: " + incidentId));

        SafariTrip safariTrip = null;
        if (dto.getTripId() != null) {
            safariTrip = safariTripRepository.findById(dto.getTripId())
                    .orElseThrow(() -> new RuntimeException("Trip not found with id: " + dto.getTripId()));
        }

        existing.setSafariTrip(safariTrip);
        existing.setIncidentDateTime(dto.getIncidentDateTime());
        existing.setIncidentType(dto.getIncidentType());
        existing.setSeverity(dto.getSeverity());
        existing.setDescription(dto.getDescription());
        existing.setResponseAction(dto.getResponseAction());
        existing.setStatus(dto.getStatus() == null ? existing.getStatus() : dto.getStatus().toUpperCase());
        existing.setRemarks(dto.getRemarks());

        EmergencyIncident updated = incidentRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteEmergencyIncident(Integer incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new RuntimeException("Incident not found with id: " + incidentId);
        }
        incidentRepository.deleteById(incidentId);
    }

    private EmergencyIncidentResponseDTO toResponseDTO(EmergencyIncident incident) {
        EmergencyIncidentResponseDTO dto = new EmergencyIncidentResponseDTO();
        dto.setIncidentId(incident.getIncidentId());
        dto.setTripId(incident.getSafariTrip() != null ? incident.getSafariTrip().getTripId() : null);
        dto.setTripSummary(incident.getSafariTrip() != null ? incident.getSafariTrip().getBoat().getBoatName() + " - " + incident.getSafariTrip().getTripDate() : null);
        dto.setIncidentDateTime(incident.getIncidentDateTime());
        dto.setIncidentType(incident.getIncidentType());
        dto.setSeverity(incident.getSeverity());
        dto.setDescription(incident.getDescription());
        dto.setResponseAction(incident.getResponseAction());
        dto.setStatus(incident.getStatus());
        dto.setRemarks(incident.getRemarks());
        dto.setCreatedAt(incident.getCreatedAt());
        dto.setUpdatedAt(incident.getUpdatedAt());
        return dto;
    }
}
