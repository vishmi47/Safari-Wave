package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.SafariTripRequestDTO;
import com.example.safariwave_backend.dto.response.SafariTripResponseDTO;
import com.example.safariwave_backend.service.SafariTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class SafariTripController {

    @Autowired
    private SafariTripService safariTripService;

    @PostMapping
    public ResponseEntity<SafariTripResponseDTO> createTrip(@RequestBody SafariTripRequestDTO dto) {
        SafariTripResponseDTO saved = safariTripService.createTrip(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SafariTripResponseDTO>> getAllTrips() {
        return ResponseEntity.ok(safariTripService.getAllTrips());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafariTripResponseDTO> getTripById(@PathVariable Integer id) {
        return safariTripService.getTripById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafariTripResponseDTO> updateTrip(@PathVariable Integer id, @RequestBody SafariTripRequestDTO dto) {
        SafariTripResponseDTO updated = safariTripService.updateTrip(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Integer id) {
        safariTripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}