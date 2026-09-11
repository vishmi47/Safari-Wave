package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.SafariTripRequestDTO;
import com.example.safariwave_backend.dto.response.SafariTripResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.service.SafariTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SafariTripServiceImpl implements SafariTripService {

    @Autowired
    private SafariTripRepository safariTripRepository;

    @Autowired
    private SafariPackageRepository safariPackageRepository;

    @Autowired
    private BoatRepository boatRepository;

    @Override
    public SafariTripResponseDTO createTrip(SafariTripRequestDTO dto) {
        SafariPackage safariPackage = safariPackageRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + dto.getPackageId()));

        Boat boat = boatRepository.findById(dto.getBoatId())
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + dto.getBoatId()));

        SafariTrip trip = new SafariTrip();
        trip.setSafariPackage(safariPackage);
        trip.setBoat(boat);
        trip.setTripDate(dto.getTripDate());
        trip.setStartTime(dto.getStartTime());
        trip.setEndTime(dto.getEndTime());
        trip.setCapacity(dto.getCapacity());
        trip.setStatus(dto.getStatus());
        trip.setRemarks(dto.getRemarks());

        SafariTrip saved = safariTripRepository.save(trip);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<SafariTripResponseDTO> getTripById(Integer tripId) {
        return safariTripRepository.findById(tripId).map(this::toResponseDTO);
    }

    @Override
    public List<SafariTripResponseDTO> getAllTrips() {
        return safariTripRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SafariTripResponseDTO updateTrip(Integer tripId, SafariTripRequestDTO dto) {
        SafariTrip existing = safariTripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found with id: " + tripId));

        SafariPackage safariPackage = safariPackageRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + dto.getPackageId()));

        Boat boat = boatRepository.findById(dto.getBoatId())
                .orElseThrow(() -> new RuntimeException("Boat not found with id: " + dto.getBoatId()));

        existing.setSafariPackage(safariPackage);
        existing.setBoat(boat);
        existing.setTripDate(dto.getTripDate());
        existing.setStartTime(dto.getStartTime());
        existing.setEndTime(dto.getEndTime());
        existing.setCapacity(dto.getCapacity());
        existing.setStatus(dto.getStatus());
        existing.setRemarks(dto.getRemarks());

        SafariTrip updated = safariTripRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteTrip(Integer tripId) {
        if (!safariTripRepository.existsById(tripId)) {
            throw new RuntimeException("Trip not found with id: " + tripId);
        }
        safariTripRepository.deleteById(tripId);
    }

    private SafariTripResponseDTO toResponseDTO(SafariTrip trip) {
        SafariTripResponseDTO dto = new SafariTripResponseDTO();
        dto.setTripId(trip.getTripId());
        dto.setPackageId(trip.getSafariPackage().getPackageId());
        dto.setPackageName(trip.getSafariPackage().getPackageName());
        dto.setBoatId(trip.getBoat().getBoatId());
        dto.setBoatName(trip.getBoat().getBoatName());
        dto.setTripDate(trip.getTripDate());
        dto.setStartTime(trip.getStartTime());
        dto.setEndTime(trip.getEndTime());
        dto.setCapacity(trip.getCapacity());
        dto.setStatus(trip.getStatus());
        dto.setRemarks(trip.getRemarks());
        dto.setCreatedAt(trip.getCreatedAt());
        dto.setUpdatedAt(trip.getUpdatedAt());
        return dto;
    }
}