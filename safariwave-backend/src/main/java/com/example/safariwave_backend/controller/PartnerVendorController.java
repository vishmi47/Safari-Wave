package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.PartnerVendorRequestDTO;
import com.example.safariwave_backend.dto.response.PartnerVendorResponseDTO;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import com.example.safariwave_backend.service.PartnerVendorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/partners-vendors")
public class PartnerVendorController {

    private final PartnerVendorService partnerVendorService;

    public PartnerVendorController(PartnerVendorService partnerVendorService) {
        this.partnerVendorService = partnerVendorService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<PartnerVendorResponseDTO> createPartnerVendor(@Valid @RequestBody PartnerVendorRequestDTO dto) {
        PartnerVendorResponseDTO created = partnerVendorService.createPartnerVendor(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<List<PartnerVendorResponseDTO>> getAllPartnerVendors() {
        return ResponseEntity.ok(partnerVendorService.getAllPartnerVendors());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<PartnerVendorResponseDTO> getPartnerVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(partnerVendorService.getPartnerVendorById(id));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<List<PartnerVendorResponseDTO>> getPartnerVendorsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(partnerVendorService.getPartnerVendorsByStatus(PartnerVendorStatus.valueOf(status.toUpperCase())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<PartnerVendorResponseDTO> updatePartnerVendor(@PathVariable Long id,
                                                                      @Valid @RequestBody PartnerVendorRequestDTO dto) {
        return ResponseEntity.ok(partnerVendorService.updatePartnerVendor(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deletePartnerVendor(@PathVariable Long id) {
        partnerVendorService.deletePartnerVendor(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<PartnerVendorResponseDTO> approvePartnerVendor(@PathVariable Long id) {
        return ResponseEntity.ok(partnerVendorService.approvePartnerVendor(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<PartnerVendorResponseDTO> rejectPartnerVendor(@PathVariable Long id) {
        return ResponseEntity.ok(partnerVendorService.rejectPartnerVendor(id));
    }

    @PostMapping("/upload-document")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Document file is required.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("application/pdf")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/png"))) {
            return ResponseEntity.badRequest().body("Only PDF, JPG, and PNG files are allowed.");
        }

        return ResponseEntity.ok("/uploads/partners-vendors/" + file.getOriginalFilename());
    }
}
