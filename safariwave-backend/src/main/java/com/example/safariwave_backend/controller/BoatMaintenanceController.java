package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.BoatMaintenanceRequestDTO;
import com.example.safariwave_backend.dto.response.BoatMaintenanceResponseDTO;
import com.example.safariwave_backend.service.BoatMaintenanceService;
import com.example.safariwave_backend.dto.request.IssueReportRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

// Role rules for maintenance records are enforced in BoatMaintenanceServiceImpl.
@RestController
@RequestMapping("/api/maintenance")
public class BoatMaintenanceController {

    private final BoatMaintenanceService maintenanceService;

    public BoatMaintenanceController(BoatMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping
    public ResponseEntity<BoatMaintenanceResponseDTO> createMaintenanceRecord(@Valid @RequestBody BoatMaintenanceRequestDTO dto) {
        BoatMaintenanceResponseDTO saved = maintenanceService.createMaintenanceRecord(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BoatMaintenanceResponseDTO>> getAllMaintenanceRecords() {
        return ResponseEntity.ok(maintenanceService.getAllMaintenanceRecords());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoatMaintenanceResponseDTO> getMaintenanceRecordById(@PathVariable Integer id) {
        return maintenanceService.getMaintenanceRecordById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/boat/{boatId}")
    public ResponseEntity<List<BoatMaintenanceResponseDTO>> getMaintenanceRecordsByBoat(@PathVariable Integer boatId) {
        return ResponseEntity.ok(maintenanceService.getMaintenanceRecordsByBoat(boatId));
    }

    // A guide (or operations) reports a boat problem; operations is notified
    @PostMapping("/issues")
    public ResponseEntity<BoatMaintenanceResponseDTO> reportIssue(@Valid @RequestBody IssueReportRequestDTO dto) {
        return new ResponseEntity<>(maintenanceService.reportIssue(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoatMaintenanceResponseDTO> updateMaintenanceRecord(@PathVariable Integer id, @Valid @RequestBody BoatMaintenanceRequestDTO dto) {
        BoatMaintenanceResponseDTO updated = maintenanceService.updateMaintenanceRecord(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenanceRecord(@PathVariable Integer id) {
        maintenanceService.deleteMaintenanceRecord(id);
        return ResponseEntity.noContent().build();
    }
}
