package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.EmergencyIncidentRequestDTO;
import com.example.safariwave_backend.dto.response.EmergencyIncidentResponseDTO;
import com.example.safariwave_backend.service.EmergencyIncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency-incidents")
public class EmergencyIncidentController {

    private final EmergencyIncidentService emergencyIncidentService;

    public EmergencyIncidentController(EmergencyIncidentService emergencyIncidentService) {
        this.emergencyIncidentService = emergencyIncidentService;
    }

    @PostMapping
    public ResponseEntity<EmergencyIncidentResponseDTO> createEmergencyIncident(@RequestBody EmergencyIncidentRequestDTO dto) {
        EmergencyIncidentResponseDTO saved = emergencyIncidentService.createEmergencyIncident(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EmergencyIncidentResponseDTO>> getAllEmergencyIncidents() {
        return ResponseEntity.ok(emergencyIncidentService.getAllEmergencyIncidents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyIncidentResponseDTO> getEmergencyIncidentById(@PathVariable Integer id) {
        return emergencyIncidentService.getEmergencyIncidentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<EmergencyIncidentResponseDTO>> getEmergencyIncidentsBySeverity(@PathVariable String severity) {
        return ResponseEntity.ok(emergencyIncidentService.getEmergencyIncidentsBySeverity(severity));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EmergencyIncidentResponseDTO>> getEmergencyIncidentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(emergencyIncidentService.getEmergencyIncidentsByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmergencyIncidentResponseDTO> updateEmergencyIncident(@PathVariable Integer id, @RequestBody EmergencyIncidentRequestDTO dto) {
        EmergencyIncidentResponseDTO updated = emergencyIncidentService.updateEmergencyIncident(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmergencyIncident(@PathVariable Integer id) {
        emergencyIncidentService.deleteEmergencyIncident(id);
        return ResponseEntity.noContent().build();
    }
}
