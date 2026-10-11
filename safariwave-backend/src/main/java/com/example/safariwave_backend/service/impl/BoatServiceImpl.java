package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.BoatRequestDTO;
import com.example.safariwave_backend.dto.response.BoatResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ConflictException;
import com.example.safariwave_backend.exception.DuplicateResourceException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.AssetRepository;
import com.example.safariwave_backend.repository.BoatMaintenanceRecordRepository;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.SafetyInspectionRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.BoatService;
import com.example.safariwave_backend.service.support.PagingSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Boats - Inventory & Asset module (spec 6.2 / B.3).
 *
 * OPERATIONS: create, read, update, retire. GUIDE and FINANCE: read (guides report boat problems through
 * BoatMaintenanceService.reportIssue). CUSTOMER: no access. ADMIN: everything, including permanent delete.
 */
@Service
public class BoatServiceImpl implements BoatService {

    static final List<String> BOAT_STATUSES = List.of("OPERATIONAL", "UNDER_MAINTENANCE", "UNSAFE", "RETIRED");
    private static final List<String> UPCOMING_TRIP_STATUSES = List.of("SCHEDULED", "SUSPENDED", "IN_PROGRESS");
    private static final String[] READ_ROLES = {Roles.OPERATIONS, Roles.GUIDE, Roles.FINANCE};

    @Autowired
    private BoatRepository boatRepository;

    @Autowired
    private SafariTripRepository safariTripRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private BoatMaintenanceRecordRepository maintenanceRepository;

    @Autowired
    private SafetyInspectionRepository safetyInspectionRepository;

    @Override
    public BoatResponseDTO createBoat(BoatRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        if (boatRepository.existsByRegistrationNumberIgnoreCase(dto.getRegistrationNumber().trim())) {
            throw new DuplicateResourceException("A boat with registration number " + dto.getRegistrationNumber().trim() + " already exists.");
        }
        Boat boat = new Boat();
        boat.setBoatName(dto.getBoatName().trim());
        boat.setRegistrationNumber(dto.getRegistrationNumber().trim());
        boat.setCapacity(dto.getCapacity());
        boat.setStatus(normalizeStatus(dto.getStatus()));
        boat.setDescription(dto.getDescription());
        boat.setCreatedBy(CurrentUser.id());

        Boat saved = boatRepository.save(boat);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<BoatResponseDTO> getBoatById(Integer boatId) {
        CurrentUser.requireAnyRole(READ_ROLES);
        return boatRepository.findById(boatId).map(this::toResponseDTO);
    }

    @Override
    public List<BoatResponseDTO> getAllBoats() {
        CurrentUser.requireAnyRole(READ_ROLES);
        return boatRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<BoatResponseDTO> searchBoats(String query, String status, int page, int size) {
        CurrentUser.requireAnyRole(READ_ROLES);
        return boatRepository.search(PagingSupport.text(query), PagingSupport.status(status, BOAT_STATUSES),
                        PagingSupport.pageRequest(page, size, Sort.by("boatName")))
                .map(this::toResponseDTO);
    }

    @Override
    @Transactional
    public BoatResponseDTO updateBoat(Integer boatId, BoatRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        Boat existing = findBoat(boatId);
        String registration = dto.getRegistrationNumber().trim();
        if (boatRepository.existsByRegistrationNumberIgnoreCaseAndBoatIdNot(registration, boatId)) {
            throw new DuplicateResourceException("A boat with registration number " + registration + " already exists.");
        }
        String status = normalizeStatus(dto.getStatus());
        List<SafariTrip> upcoming = upcomingTrips(boatId);
        if ("RETIRED".equals(status) && !"RETIRED".equals(existing.getStatus())) {
            requireNoUpcomingTrips(existing, upcoming);
        }
        int largestTrip = upcoming.stream().mapToInt(trip -> trip.getCapacity() == null ? 0 : trip.getCapacity()).max().orElse(0);
        if (dto.getCapacity() < largestTrip) {
            throw new BadRequestException("An upcoming trip on this boat has capacity " + largestTrip
                    + ". Lower that trip's capacity before reducing the boat's capacity.");
        }

        existing.setBoatName(dto.getBoatName().trim());
        existing.setRegistrationNumber(registration);
        existing.setCapacity(dto.getCapacity());
        existing.setStatus(status);
        existing.setDescription(dto.getDescription());

        Boat updated = boatRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteBoat(Integer boatId, boolean permanent) {
        Boat boat = findBoat(boatId);
        if (permanent) {
            CurrentUser.requireAnyRole(); // ADMIN only
            if (safariTripRepository.existsByBoat_BoatId(boatId) || assetRepository.existsByBoats_BoatId(boatId)
                    || assetRepository.existsByBoat_BoatId(boatId)
                    || maintenanceRepository.existsByBoat_BoatId(boatId) || safetyInspectionRepository.existsByBoat_BoatId(boatId)) {
                throw new ConflictException(boat.getBoatName() + " has trips, assets or maintenance history. Retire it instead.");
            }
            boatRepository.delete(boat);
            return;
        }
        // Spec B.3: prefer a RETIRED status over hard delete
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        requireNoUpcomingTrips(boat, upcomingTrips(boatId));
        boat.setStatus("RETIRED");
        boatRepository.save(boat);
    }

    private List<SafariTrip> upcomingTrips(Integer boatId) {
        return safariTripRepository.findByBoat_BoatIdAndTripDateGreaterThanEqualAndStatusIn(boatId, LocalDate.now(), UPCOMING_TRIP_STATUSES);
    }

    private void requireNoUpcomingTrips(Boat boat, List<SafariTrip> upcoming) {
        if (!upcoming.isEmpty()) {
            throw new BadRequestException(boat.getBoatName() + " has " + upcoming.size()
                    + " upcoming trip(s). Assign another boat to them before retiring it.");
        }
    }

    private Boat findBoat(Integer boatId) {
        return boatRepository.findById(boatId)
                .orElseThrow(() -> new ResourceNotFoundException("Boat not found with id: " + boatId));
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
        dto.setCreatedBy(boat.getCreatedBy());
        return dto;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "OPERATIONAL" : status.trim().toUpperCase();
        if ("ACTIVE".equals(normalized)) normalized = "OPERATIONAL";
        if ("MAINTENANCE".equals(normalized)) normalized = "UNDER_MAINTENANCE";
        if (!BOAT_STATUSES.contains(normalized)) {
            throw new BadRequestException("Unsupported boat status: " + status);
        }
        return normalized;
    }
}
