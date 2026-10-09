package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.PartnerVendorRequestDTO;
import com.example.safariwave_backend.dto.response.PartnerVendorResponseDTO;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.service.PartnerVendorService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Writes are ADMIN only; reads are filtered by partner type per role in PartnerVendorServiceImpl.
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
    public ResponseEntity<List<PartnerVendorResponseDTO>> getAllPartnerVendors() {
        return ResponseEntity.ok(partnerVendorService.getAllPartnerVendors());
    }

    // Paged list with search: GET /api/partners-vendors/search?q=&status=APPROVED&type=HOTEL&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<PagedModel<PartnerVendorResponseDTO>> searchPartnerVendors(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(partnerVendorService.searchPartnerVendors(q, status, type, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerVendorResponseDTO> getPartnerVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(partnerVendorService.getPartnerVendorById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PartnerVendorResponseDTO>> getPartnerVendorsByStatus(@PathVariable String status) {
        try {
            return ResponseEntity.ok(partnerVendorService.getPartnerVendorsByStatus(PartnerVendorStatus.valueOf(status.toUpperCase())));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown status: " + status);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<PartnerVendorResponseDTO> updatePartnerVendor(@PathVariable Long id,
                                                                      @Valid @RequestBody PartnerVendorRequestDTO dto) {
        return ResponseEntity.ok(partnerVendorService.updatePartnerVendor(id, dto));
    }

    // Marks the partner INACTIVE; ?permanent=true removes a partner that is already inactive or terminated
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deletePartnerVendor(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean permanent) {
        partnerVendorService.deletePartnerVendor(id, permanent);
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

    @PutMapping("/{id}/terminate")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<PartnerVendorResponseDTO> terminatePartnerVendor(@PathVariable Long id) {
        return ResponseEntity.ok(partnerVendorService.terminatePartnerVendor(id));
    }

    // Stores the agreement document (PDF, JPG or PNG) and returns the URL to save as documentUrl
    @PostMapping("/upload-document")
    @PreAuthorize("hasAnyRole('GENERAL_MANAGER', 'ADMIN')")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(partnerVendorService.storeDocument(file));
    }

    // Agreement documents are private: only users who may see the partner can download them
    @GetMapping("/documents/{filename}")
    public ResponseEntity<Resource> getDocument(@PathVariable String filename) {
        MediaType type = filename.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : filename.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(partnerVendorService.loadDocument(filename));
    }
}
