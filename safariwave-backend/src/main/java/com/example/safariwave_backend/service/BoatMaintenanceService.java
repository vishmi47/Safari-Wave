package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.BoatMaintenanceRequestDTO;
import com.example.safariwave_backend.dto.response.BoatMaintenanceResponseDTO;

import java.util.List;
import java.util.Optional;

public interface BoatMaintenanceService {

    BoatMaintenanceResponseDTO createMaintenanceRecord(BoatMaintenanceRequestDTO dto);

    Optional<BoatMaintenanceResponseDTO> getMaintenanceRecordById(Integer maintenanceId);

    List<BoatMaintenanceResponseDTO> getAllMaintenanceRecords();

    List<BoatMaintenanceResponseDTO> getMaintenanceRecordsByBoat(Integer boatId);

    BoatMaintenanceResponseDTO updateMaintenanceRecord(Integer maintenanceId, BoatMaintenanceRequestDTO dto);

    void deleteMaintenanceRecord(Integer maintenanceId);
}
