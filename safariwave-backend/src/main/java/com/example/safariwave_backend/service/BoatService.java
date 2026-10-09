package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.BoatRequestDTO;
import com.example.safariwave_backend.dto.response.BoatResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface BoatService {

    BoatResponseDTO createBoat(BoatRequestDTO dto);

    Optional<BoatResponseDTO> getBoatById(Integer boatId);

    List<BoatResponseDTO> getAllBoats();

    Page<BoatResponseDTO> searchBoats(String query, String status, int page, int size);

    BoatResponseDTO updateBoat(Integer boatId, BoatRequestDTO dto);

    /** Retires the boat (status RETIRED). With permanent=true an ADMIN removes an unused boat completely. */
    void deleteBoat(Integer boatId, boolean permanent);
}
