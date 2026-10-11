package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.EmergencyIncidentRequestDTO;
import com.example.safariwave_backend.dto.response.EmergencyIncidentResponseDTO;
import com.example.safariwave_backend.entity.EmergencyIncident;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.EmergencyIncidentRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.WeatherAlertRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.EmergencyIncidentService;
import com.example.safariwave_backend.service.NotificationService;
import com.example.safariwave_backend.service.support.PagingSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Emergency incidents - Weather, Safety & Emergency module (spec 6.4 / B.3).
 * GUIDE and OPERATIONS report incidents; a guide updates only their own; OPERATIONS updates all and deletes
 * invalid or duplicate records. HIGH and CRITICAL incidents notify the operations team immediately.
 */
@Service
public class EmergencyIncidentServiceImpl implements EmergencyIncidentService {

    static final List<String> INCIDENT_STATUSES = List.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");

    private final EmergencyIncidentRepository incidentRepository;
    private final SafariTripRepository safariTripRepository;
    private final WeatherAlertRepository weatherAlertRepository;
    private final NotificationService notificationService;

    public EmergencyIncidentServiceImpl(EmergencyIncidentRepository incidentRepository, SafariTripRepository safariTripRepository,
                                        WeatherAlertRepository weatherAlertRepository, NotificationService notificationService) {
        this.incidentRepository = incidentRepository;
        this.safariTripRepository = safariTripRepository;
        this.weatherAlertRepository = weatherAlertRepository;
        this.notificationService = notificationService;
    }

    @Override
    public EmergencyIncidentResponseDTO createEmergencyIncident(EmergencyIncidentRequestDTO dto) {
        SafetyPermissions.requireCanRecord();
        EmergencyIncident incident = new EmergencyIncident();
        copyRequestToEntity(dto, incident, "OPEN");
        incident.setCreatedBy(CurrentUser.id());

        EmergencyIncident saved = incidentRepository.save(incident);
        if (List.of("HIGH", "CRITICAL").contains(saved.getSeverity())) {
            notificationService.notifyRole(Roles.OPERATIONS, "EMERGENCY_INCIDENT",
                    saved.getSeverity().toLowerCase(Locale.ROOT) + " " + saved.getIncidentType() + " reported"
                            + (saved.getSafariTrip() != null ? " on trip #" + saved.getSafariTrip().getTripId() : "")
                            + ": " + saved.getDescription());
        }
        return toResponseDTO(saved);
    }

    @Override
    public Optional<EmergencyIncidentResponseDTO> getEmergencyIncidentById(Integer incidentId) {
        SafetyPermissions.requireStaffRead();
        return incidentRepository.findById(incidentId).map(this::toResponseDTO);
    }

    @Override
    public List<EmergencyIncidentResponseDTO> getAllEmergencyIncidents() {
        SafetyPermissions.requireStaffRead();
        return incidentRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<EmergencyIncidentResponseDTO> getEmergencyIncidentsBySeverity(String severity) {
        SafetyPermissions.requireStaffRead();
        return incidentRepository.findBySeverity(severity.toUpperCase()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<EmergencyIncidentResponseDTO> getEmergencyIncidentsByStatus(String status) {
        SafetyPermissions.requireStaffRead();
        return incidentRepository.findByStatus(status.toUpperCase()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public Page<EmergencyIncidentResponseDTO> searchEmergencyIncidents(String query, String status, int page, int size) {
        SafetyPermissions.requireStaffRead();
        return incidentRepository.search(PagingSupport.text(query), PagingSupport.status(status, INCIDENT_STATUSES),
                        PagingSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "incidentDateTime")))
                .map(this::toResponseDTO);
    }

    @Override
    public EmergencyIncidentResponseDTO updateEmergencyIncident(Integer incidentId, EmergencyIncidentRequestDTO dto) {
        EmergencyIncident existing = findIncident(incidentId);
        SafetyPermissions.requireCanEdit(existing.getCreatedBy());
        copyRequestToEntity(dto, existing, existing.getStatus());

        EmergencyIncident updated = incidentRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteEmergencyIncident(Integer incidentId) {
        SafetyPermissions.requireCanDelete();
        EmergencyIncident incident = findIncident(incidentId);
        weatherAlertRepository.findDistinctByAffectedIncidents_IncidentId(incidentId).forEach(alert -> {
            alert.getAffectedIncidents().removeIf(affected -> affected.getIncidentId().equals(incidentId));
            weatherAlertRepository.save(alert);
        });
        incidentRepository.delete(incident);
    }

    private void copyRequestToEntity(EmergencyIncidentRequestDTO dto, EmergencyIncident incident, String defaultStatus) {
        SafariTrip safariTrip = null;
        if (dto.getTripId() != null) {
            safariTrip = safariTripRepository.findById(dto.getTripId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + dto.getTripId()));
        }
        incident.setSafariTrip(safariTrip);
        incident.setIncidentDateTime(dto.getIncidentDateTime());
        incident.setIncidentType(dto.getIncidentType().trim());
        incident.setSeverity(SafetyPermissions.oneOf(dto.getSeverity(), SafetyPermissions.SEVERITIES, "Severity", null));
        incident.setDescription(dto.getDescription().trim());
        incident.setResponseAction(dto.getResponseAction());
        incident.setStatus(SafetyPermissions.oneOf(dto.getStatus(), INCIDENT_STATUSES, "Status", defaultStatus));
        incident.setRemarks(dto.getRemarks());
    }

    private EmergencyIncident findIncident(Integer incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency incident not found with id: " + incidentId));
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
        dto.setCreatedBy(incident.getCreatedBy());
        return dto;
    }
}
