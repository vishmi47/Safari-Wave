package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.SafariPackageRequestDTO;
import com.example.safariwave_backend.dto.response.SafariPackageResponseDTO;
import com.example.safariwave_backend.service.SafariPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
public class SafariPackageController {

    @Autowired
    private SafariPackageService safariPackageService;

    @PostMapping
    public ResponseEntity<SafariPackageResponseDTO> createPackage(@RequestBody SafariPackageRequestDTO dto) {
        SafariPackageResponseDTO saved = safariPackageService.createPackage(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SafariPackageResponseDTO>> getAllPackages() {
        return ResponseEntity.ok(safariPackageService.getAllPackages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafariPackageResponseDTO> getPackageById(@PathVariable Integer id) {
        return safariPackageService.getPackageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafariPackageResponseDTO> updatePackage(@PathVariable Integer id, @RequestBody SafariPackageRequestDTO dto) {
        SafariPackageResponseDTO updated = safariPackageService.updatePackage(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable Integer id) {
        safariPackageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}