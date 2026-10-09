package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.SafariTripRequestDTO;
import com.example.safariwave_backend.dto.request.TripAssignmentRequestDTO;
import com.example.safariwave_backend.dto.response.SafariTripResponseDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;

import java.util.List;
import java.util.Optional;

public interface SafariTripService {

    SafariTripResponseDTO createTrip(SafariTripRequestDTO dto);

    Optional<SafariTripResponseDTO> getTripById(Integer tripId);

    List<SafariTripResponseDTO> getAllTrips();

    /** Trips assigned to the logged-in guide. */
    List<SafariTripResponseDTO> getAssignedTrips();

    /** Users who can be assigned as a trip guide (for the Operations Supervisor's form). */
    List<UserResponseDTO> getGuides();

    SafariTripResponseDTO updateTrip(Integer tripId, SafariTripRequestDTO dto);

    /** Operations Supervisor assigns the boat and/or guide. */
    SafariTripResponseDTO assignTrip(Integer tripId, TripAssignmentRequestDTO dto);

    /** Operations Supervisor sets any status; the assigned guide may set IN_PROGRESS or COMPLETED. */
    SafariTripResponseDTO updateTripStatus(Integer tripId, String status, String remarks);

    SafariTripResponseDTO suspendTrip(Integer tripId, String remarks);

    SafariTripResponseDTO cancelTrip(Integer tripId, String remarks);

    void deleteTrip(Integer tripId);
}
