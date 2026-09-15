package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.SafetyInspectionRequestDTO;
import com.example.safariwave_backend.dto.response.SafetyInspectionResponseDTO;
import com.example.safariwave_backend.service.SafetyInspectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety-inspections")
public class SafetyInspectionController {

    private final SafetyInspectionService safetyInspectionService;

    public SafetyInspectionController(SafetyInspectionService safetyInspectionService) {
        this.safetyInspectionService = safetyInspectionService;
    }

    @PostMapping
    public ResponseEntity<SafetyInspectionResponseDTO> createSafetyInspection(@RequestBody SafetyInspectionRequestDTO dto) {
        SafetyInspectionResponseDTO saved = safetyInspectionService.createSafetyInspection(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SafetyInspectionResponseDTO>> getAllSafetyInspections() {
        return ResponseEntity.ok(safetyInspectionService.getAllSafetyInspections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafetyInspectionResponseDTO> getSafetyInspectionById(@PathVariable Integer id) {
        return safetyInspectionService.getSafetyInspectionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/boat/{boatId}")
    public ResponseEntity<List<SafetyInspectionResponseDTO>> getSafetyInspectionsByBoat(@PathVariable Integer boatId) {
        return ResponseEntity.ok(safetyInspectionService.getSafetyInspectionsByBoat(boatId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafetyInspectionResponseDTO> updateSafetyInspection(@PathVariable Integer id, @RequestBody SafetyInspectionRequestDTO dto) {
        SafetyInspectionResponseDTO updated = safetyInspectionService.updateSafetyInspection(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSafetyInspection(@PathVariable Integer id) {
        safetyInspectionService.deleteSafetyInspection(id);
        return ResponseEntity.noContent().build();
    }
}
