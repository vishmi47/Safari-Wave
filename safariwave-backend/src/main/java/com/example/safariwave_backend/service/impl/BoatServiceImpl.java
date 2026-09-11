package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.BoatRequestDTO;
import com.example.safariwave_backend.dto.response.BoatResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.service.BoatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BoatServiceImpl implements BoatService {

    @Autowired
    private BoatRepository boatRepository;

    @Override
    public BoatResponseDTO createBoat(BoatRequestDTO dto) {
        Boat boat = new Boat();
        boat.setBoatName(dto.getBoatName());
        boat.setRegistrationNumber(dto.getRegistrationNumber());
        boat.setCapacity(dto.getCapacity());
        boat.setStatus(dto.getStatus());
        boat.setDescription(dto.getDescription());

        Boat saved = boatRepository.save(boat);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<BoatResponseDTO> getBoatById(Integer boatId) {
        return boatRepository.findById(boatId).map(this::toResponseDTO);
    }

    @Override
    public List<BoatResponseDTO> getAllBoats() {
        return boatRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BoatResponseDTO updateBoat(Integer boatId, BoatRequestDTO dto) {
        Boat existing = boatRepository.findById(boatId)
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + boatId));

        existing.setBoatName(dto.getBoatName());
        existing.setRegistrationNumber(dto.getRegistrationNumber());
        existing.setCapacity(dto.getCapacity());
        existing.setStatus(dto.getStatus());
        existing.setDescription(dto.getDescription());

        Boat updated = boatRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteBoat(Integer boatId) {
        if (!boatRepository.existsById(boatId)) {
            throw new RuntimeException("Boat not found with id: " + boatId);
        }
        boatRepository.deleteById(boatId);
    }

    private BoatResponseDTO toResponseDTO(Boat boat) {
        BoatResponseDTO dto = new BoatResponseDTO();
        dto.setBoatId(boat.getBoatId());
        dto.setBoatName(boat.getBoatName());
        dto.setRegistrationNumber(boat.getRegistrationNumber());
        dto.setCapacity(boat.getCapacity());
        dto.setStatus(boat.getStatus());
        dto.setDescription(boat.getDescription());
        dto.setCreatedAt(boat.getCreatedAt());
        dto.setUpdatedAt(boat.getUpdatedAt());
        return dto;
    }
}