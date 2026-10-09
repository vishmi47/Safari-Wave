package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.SafariPackageRequestDTO;
import com.example.safariwave_backend.dto.response.SafariPackageResponseDTO;
import com.example.safariwave_backend.service.SafariPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

// Role and ownership rules for packages are enforced in SafariPackageServiceImpl.
@RestController
@RequestMapping("/api/packages")
public class SafariPackageController {

    @Autowired
    private SafariPackageService safariPackageService;

    @PostMapping
    public ResponseEntity<SafariPackageResponseDTO> createPackage(@Valid @RequestBody SafariPackageRequestDTO dto) {
        SafariPackageResponseDTO saved = safariPackageService.createPackage(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SafariPackageResponseDTO>> getAllPackages() {
        return ResponseEntity.ok(safariPackageService.getAllPackages());
    }

    // Paged list with search: GET /api/packages/search?q=lagoon&status=ACTIVE&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<PagedModel<SafariPackageResponseDTO>> searchPackages(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(safariPackageService.searchPackages(q, status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafariPackageResponseDTO> getPackageById(@PathVariable Integer id) {
        return safariPackageService.getPackageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafariPackageResponseDTO> updatePackage(@PathVariable Integer id, @Valid @RequestBody SafariPackageRequestDTO dto) {
        SafariPackageResponseDTO updated = safariPackageService.updatePackage(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable Integer id) {
        safariPackageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}
