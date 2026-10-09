package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.SafariTripRequestDTO;
import com.example.safariwave_backend.dto.request.StatusUpdateRequestDTO;
import com.example.safariwave_backend.dto.request.TripAssignmentRequestDTO;
import com.example.safariwave_backend.dto.response.SafariTripResponseDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;
import com.example.safariwave_backend.service.SafariTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

// Role rules for trips are enforced in SafariTripServiceImpl.
@RestController
@RequestMapping("/api/trips")
public class SafariTripController {

    @Autowired
    private SafariTripService safariTripService;

    @PostMapping
    public ResponseEntity<SafariTripResponseDTO> createTrip(@Valid @RequestBody SafariTripRequestDTO dto) {
        SafariTripResponseDTO saved = safariTripService.createTrip(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SafariTripResponseDTO>> getAllTrips() {
        return ResponseEntity.ok(safariTripService.getAllTrips());
    }

    // Trips assigned to the logged-in guide
    @GetMapping("/assigned")
    public ResponseEntity<List<SafariTripResponseDTO>> getAssignedTrips() {
        return ResponseEntity.ok(safariTripService.getAssignedTrips());
    }

    // Guides the Operations Supervisor can assign
    @GetMapping("/guides")
    public ResponseEntity<List<UserResponseDTO>> getGuides() {
        return ResponseEntity.ok(safariTripService.getGuides());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafariTripResponseDTO> getTripById(@PathVariable Integer id) {
        return safariTripService.getTripById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafariTripResponseDTO> updateTrip(@PathVariable Integer id, @Valid @RequestBody SafariTripRequestDTO dto) {
        SafariTripResponseDTO updated = safariTripService.updateTrip(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/assignment")
    public ResponseEntity<SafariTripResponseDTO> assignTrip(@PathVariable Integer id, @RequestBody TripAssignmentRequestDTO dto) {
        return ResponseEntity.ok(safariTripService.assignTrip(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SafariTripResponseDTO> updateTripStatus(@PathVariable Integer id, @Valid @RequestBody StatusUpdateRequestDTO dto) {
        return ResponseEntity.ok(safariTripService.updateTripStatus(id, dto.getStatus(), dto.getRemarks()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Integer id) {
        safariTripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<SafariTripResponseDTO> suspendTrip(@PathVariable Integer id,
                                                               @RequestBody(required = false) java.util.Map<String, String> body) {
        String remarks = body == null ? null : body.get("remarks");
        return ResponseEntity.ok(safariTripService.suspendTrip(id, remarks));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SafariTripResponseDTO> cancelTrip(@PathVariable Integer id,
                                                              @RequestBody(required = false) java.util.Map<String, String> body) {
        String remarks = body == null ? null : body.get("remarks");
        return ResponseEntity.ok(safariTripService.cancelTrip(id, remarks));
    }
}
