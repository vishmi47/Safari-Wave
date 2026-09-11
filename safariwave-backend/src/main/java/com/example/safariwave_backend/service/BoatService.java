package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.BoatRequestDTO;
import com.example.safariwave_backend.dto.response.BoatResponseDTO;

import java.util.List;
import java.util.Optional;

public interface BoatService {

    BoatResponseDTO createBoat(BoatRequestDTO dto);

    Optional<BoatResponseDTO> getBoatById(Integer boatId);

    List<BoatResponseDTO> getAllBoats();

    BoatResponseDTO updateBoat(Integer boatId, BoatRequestDTO dto);

    void deleteBoat(Integer boatId);
}