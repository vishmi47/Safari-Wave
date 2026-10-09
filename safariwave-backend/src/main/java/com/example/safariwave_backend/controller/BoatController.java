package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.BoatRequestDTO;
import com.example.safariwave_backend.dto.response.BoatResponseDTO;
import com.example.safariwave_backend.service.BoatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

// Role rules for boats are enforced in BoatServiceImpl.
@RestController
@RequestMapping("/api/boats")
public class BoatController {

    @Autowired
    private BoatService boatService;

    @PostMapping
    public ResponseEntity<BoatResponseDTO> createBoat(@Valid @RequestBody BoatRequestDTO dto) {
        BoatResponseDTO saved = boatService.createBoat(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BoatResponseDTO>> getAllBoats() {
        return ResponseEntity.ok(boatService.getAllBoats());
    }

    // Paged list with search: GET /api/boats/search?q=&status=&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<PagedModel<BoatResponseDTO>> searchBoats(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(boatService.searchBoats(q, status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoatResponseDTO> getBoatById(@PathVariable Integer id) {
        return boatService.getBoatById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoatResponseDTO> updateBoat(@PathVariable Integer id, @Valid @RequestBody BoatRequestDTO dto) {
        BoatResponseDTO updated = boatService.updateBoat(id, dto);
        return ResponseEntity.ok(updated);
    }

    // Retires the boat; ADMIN may pass ?permanent=true to remove an unused boat completely
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoat(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean permanent) {
        boatService.deleteBoat(id, permanent);
        return ResponseEntity.noContent().build();
    }
}