package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.BoatRequestDTO;
import com.example.safariwave_backend.dto.response.BoatResponseDTO;
import com.example.safariwave_backend.service.BoatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boats")
public class BoatController {

    @Autowired
    private BoatService boatService;

    @PostMapping
    public ResponseEntity<BoatResponseDTO> createBoat(@RequestBody BoatRequestDTO dto) {
        BoatResponseDTO saved = boatService.createBoat(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BoatResponseDTO>> getAllBoats() {
        return ResponseEntity.ok(boatService.getAllBoats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoatResponseDTO> getBoatById(@PathVariable Integer id) {
        return boatService.getBoatById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoatResponseDTO> updateBoat(@PathVariable Integer id, @RequestBody BoatRequestDTO dto) {
        BoatResponseDTO updated = boatService.updateBoat(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoat(@PathVariable Integer id) {
        boatService.deleteBoat(id);
        return ResponseEntity.noContent().build();
    }
}