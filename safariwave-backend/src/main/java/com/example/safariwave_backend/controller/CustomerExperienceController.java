package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.ComplaintResponseRequestDTO;
import com.example.safariwave_backend.dto.request.ComplaintStatusUpdateRequestDTO;
import com.example.safariwave_backend.dto.request.CustomerExperienceRequestDTO;
import com.example.safariwave_backend.dto.response.CustomerExperienceResponseDTO;
import com.example.safariwave_backend.service.CustomerExperienceService;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Ownership and role rules are enforced in CustomerExperienceServiceImpl.
@RestController
@RequestMapping("/api/customer-experiences")
public class CustomerExperienceController {

    private final CustomerExperienceService customerExperienceService;

    public CustomerExperienceController(CustomerExperienceService customerExperienceService) {
        this.customerExperienceService = customerExperienceService;
    }

    @PostMapping
    public ResponseEntity<CustomerExperienceResponseDTO> createExperience(@Valid @RequestBody CustomerExperienceRequestDTO dto) {
        CustomerExperienceResponseDTO created = customerExperienceService.createExperience(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'CUSTOMER_RELATIONS')")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getAllExperiences() {
        return ResponseEntity.ok(customerExperienceService.getAllExperiences());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerExperienceResponseDTO> getExperienceById(@PathVariable Integer id) {
        return customerExperienceService.getExperienceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getExperiencesByCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.ok(customerExperienceService.getExperiencesByCustomer(customerId));
    }

    @GetMapping("/type/{experienceType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'CUSTOMER_RELATIONS')")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getExperiencesByType(@PathVariable String experienceType) {
        return ResponseEntity.ok(customerExperienceService.getExperiencesByType(experienceType));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'CUSTOMER_RELATIONS')")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getExperiencesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(customerExperienceService.getExperiencesByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerExperienceResponseDTO> updateExperience(@PathVariable Integer id,
                                                                       @Valid @RequestBody CustomerExperienceRequestDTO dto) {
        return ResponseEntity.ok(customerExperienceService.updateExperience(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExperience(@PathVariable Integer id) {
        customerExperienceService.deleteExperience(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/complaints")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'CUSTOMER_RELATIONS')")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getAllComplaints() {
        return ResponseEntity.ok(customerExperienceService.getAllComplaints());
    }

    @GetMapping("/complaints/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'CUSTOMER_RELATIONS')")
    public ResponseEntity<CustomerExperienceResponseDTO> getComplaintById(@PathVariable Integer id) {
        return customerExperienceService.getComplaintById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/complaints/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'CUSTOMER_RELATIONS')")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getComplaintsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(customerExperienceService.getComplaintsByStatus(status));
    }

    @GetMapping("/complaints/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'GENERAL_MANAGER', 'CUSTOMER_RELATIONS')")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getComplaintsByDateRange(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(customerExperienceService.getComplaintsByDateRange(from, to));
    }

    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<CustomerExperienceResponseDTO> updateComplaintStatus(@PathVariable Integer id,
                                                                            @Valid @RequestBody ComplaintStatusUpdateRequestDTO dto) {
        return ResponseEntity.ok(customerExperienceService.updateComplaintStatus(id, dto));
    }

    @PutMapping("/complaints/{id}/response")
    public ResponseEntity<CustomerExperienceResponseDTO> respondToComplaint(@PathVariable Integer id,
                                                                         @Valid @RequestBody ComplaintResponseRequestDTO dto) {
        return ResponseEntity.ok(customerExperienceService.respondToComplaint(id, dto));
    }

    @PutMapping("/complaints/{id}/escalate")
    public ResponseEntity<CustomerExperienceResponseDTO> escalateComplaint(@PathVariable Integer id,
                                                                         @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(customerExperienceService.escalateComplaint(id, reason));
    }

    @PutMapping("/complaints/{id}/notes")
    public ResponseEntity<CustomerExperienceResponseDTO> addComplaintNotes(@PathVariable Integer id,
                                                                        @RequestParam String notes) {
        return ResponseEntity.ok(customerExperienceService.addComplaintNotes(id, notes));
    }

    // Paged list with search, scoped by role: GET /api/customer-experiences/search?q=&status=&type=&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<PagedModel<CustomerExperienceResponseDTO>> searchExperiences(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(customerExperienceService.searchExperiences(q, status, type, page, size)));
    }

    // Marketing: rated records (customer details hidden) and aggregate stats
    @GetMapping("/ratings")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getRatings() {
        return ResponseEntity.ok(customerExperienceService.getRatings());
    }

    @GetMapping("/ratings/summary")
    public ResponseEntity<Map<String, Object>> getRatingSummary() {
        return ResponseEntity.ok(customerExperienceService.getRatingSummary());
    }

    // Guide: feedback and ratings on trips they led
    @GetMapping("/my-trips")
    public ResponseEntity<List<CustomerExperienceResponseDTO>> getFeedbackForMyTrips() {
        return ResponseEntity.ok(customerExperienceService.getFeedbackForMyTrips());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Object> getDashboardSummary() {
        return ResponseEntity.ok(customerExperienceService.getDashboardSummary());
    }
}
