package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.SafariTripRequestDTO;
import com.example.safariwave_backend.dto.response.SafariTripResponseDTO;

import java.util.List;
import java.util.Optional;

public interface SafariTripService {

    SafariTripResponseDTO createTrip(SafariTripRequestDTO dto);

    Optional<SafariTripResponseDTO> getTripById(Integer tripId);

    List<SafariTripResponseDTO> getAllTrips();

    SafariTripResponseDTO updateTrip(Integer tripId, SafariTripRequestDTO dto);

    void deleteTrip(Integer tripId);
}