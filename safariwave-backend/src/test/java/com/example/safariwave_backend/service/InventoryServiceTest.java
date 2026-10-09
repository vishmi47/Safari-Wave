package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.AssetRequestDTO;
import com.example.safariwave_backend.dto.request.BoatRequestDTO;
import com.example.safariwave_backend.dto.request.IssueReportRequestDTO;
import com.example.safariwave_backend.entity.Asset;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.BoatMaintenanceRecord;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ConflictException;
import com.example.safariwave_backend.exception.DuplicateResourceException;
import com.example.safariwave_backend.repository.AssetRepository;
import com.example.safariwave_backend.repository.BoatMaintenanceRecordRepository;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.SafetyInspectionRepository;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.impl.AssetServiceImpl;
import com.example.safariwave_backend.service.impl.BoatMaintenanceServiceImpl;
import com.example.safariwave_backend.service.impl.BoatServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Inventory & Asset module (spec 6.2 / B.3): boats, assets and maintenance records. */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private BoatRepository boatRepository;

    @Mock
    private SafariTripRepository safariTripRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private BoatMaintenanceRecordRepository maintenanceRepository;

    @Mock
    private SafetyInspectionRepository safetyInspectionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BoatServiceImpl boatService;

    private AssetServiceImpl assetService;
    private BoatMaintenanceServiceImpl maintenanceService;
    private Boat boat;

    @BeforeEach
    void setUp() {
        assetService = new AssetServiceImpl(assetRepository, boatRepository, notificationService);
        maintenanceService = new BoatMaintenanceServiceImpl(maintenanceRepository, boatRepository, notificationService);
        boat = new Boat();
        boat.setBoatId(4);
        boat.setBoatName("Heron");
        boat.setRegistrationNumber("WL-001");
        boat.setCapacity(12);
        boat.setStatus("OPERATIONAL");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(Integer userId, String role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, "user" + userId + "@safariwave.test", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Roles.authoritiesFor(role)));
    }

    private BoatRequestDTO boatRequest(String registration, int capacity, String status) {
        BoatRequestDTO dto = new BoatRequestDTO();
        dto.setBoatName("Heron");
        dto.setRegistrationNumber(registration);
        dto.setCapacity(capacity);
        dto.setStatus(status);
        return dto;
    }

    // ---- boats ----

    @Test
    void operations_canAddBoat() {
        loginAs(3, Roles.OPERATIONS);
        when(boatRepository.existsByRegistrationNumberIgnoreCase("WL-009")).thenReturn(false);
        when(boatRepository.save(any(Boat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("OPERATIONAL", boatService.createBoat(boatRequest("WL-009", 10, "active")).getStatus());
    }

    @Test
    void customer_hasNoInventoryAccess() {
        loginAs(10, Roles.CUSTOMER);

        assertThrows(AccessDeniedException.class, () -> boatService.getAllBoats());
        assertThrows(AccessDeniedException.class, () -> assetService.getAllAssets());
        assertThrows(AccessDeniedException.class, () -> maintenanceService.getAllMaintenanceRecords());
    }

    @Test
    void finance_canReadButNotEdit() {
        loginAs(6, Roles.FINANCE);
        when(boatRepository.findAll()).thenReturn(List.of(boat));

        assertEquals(1, boatService.getAllBoats().size());
        assertThrows(AccessDeniedException.class, () -> boatService.updateBoat(4, boatRequest("WL-001", 12, "OPERATIONAL")));
    }

    @Test
    void duplicateRegistration_isRejected() {
        loginAs(3, Roles.OPERATIONS);
        when(boatRepository.existsByRegistrationNumberIgnoreCase("WL-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> boatService.createBoat(boatRequest("WL-001", 10, "OPERATIONAL")));
    }

    @Test
    void invalidBoatStatus_isRejected() {
        loginAs(3, Roles.OPERATIONS);
        when(boatRepository.existsByRegistrationNumberIgnoreCase("WL-010")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> boatService.createBoat(boatRequest("WL-010", 10, "SUNK")));
    }

    @Test
    void retiringBoat_setsStatusInsteadOfDeleting() {
        loginAs(3, Roles.OPERATIONS);
        when(boatRepository.findById(4)).thenReturn(Optional.of(boat));
        when(safariTripRepository.findByBoat_BoatIdAndTripDateGreaterThanEqualAndStatusIn(eq(4), any(), anyList())).thenReturn(List.of());

        boatService.deleteBoat(4, false);

        assertEquals("RETIRED", boat.getStatus());
        verify(boatRepository, never()).delete(any());
    }

    @Test
    void boatWithUpcomingTrips_cannotBeRetired() {
        loginAs(3, Roles.OPERATIONS);
        when(boatRepository.findById(4)).thenReturn(Optional.of(boat));
        when(safariTripRepository.findByBoat_BoatIdAndTripDateGreaterThanEqualAndStatusIn(eq(4), any(), anyList()))
                .thenReturn(List.of(new SafariTrip()));

        assertThrows(BadRequestException.class, () -> boatService.deleteBoat(4, false));
        assertEquals("OPERATIONAL", boat.getStatus());
    }

    @Test
    void operations_cannotPermanentlyDelete_andAdminCannotDeleteUsedBoat() {
        loginAs(3, Roles.OPERATIONS);
        when(boatRepository.findById(4)).thenReturn(Optional.of(boat));
        assertThrows(AccessDeniedException.class, () -> boatService.deleteBoat(4, true));

        loginAs(1, Roles.ADMIN);
        when(safariTripRepository.existsByBoat_BoatId(4)).thenReturn(true);
        assertThrows(ConflictException.class, () -> boatService.deleteBoat(4, true));
        verify(boatRepository, never()).delete(any());
    }

    // ---- assets ----

    @Test
    void guide_canReportConditionButNotEditAsset() {
        loginAs(7, Roles.GUIDE);
        Asset jacket = new Asset();
        jacket.setAssetId(9);
        jacket.setAssetName("Life jacket L");
        when(assetRepository.findById(9)).thenReturn(Optional.of(jacket));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("DAMAGED", assetService.updateCondition(9, "damaged", "Torn strap").getConditionStatus());
        verify(notificationService).notifyRole(eq(Roles.OPERATIONS), eq("ASSET_CONDITION"), anyString());

        AssetRequestDTO dto = new AssetRequestDTO();
        assertThrows(AccessDeniedException.class, () -> assetService.updateAsset(9, dto));
    }

    @Test
    void invalidAssetCondition_isRejected() {
        loginAs(3, Roles.OPERATIONS);
        when(assetRepository.findById(9)).thenReturn(Optional.of(new Asset()));

        assertThrows(BadRequestException.class, () -> assetService.updateCondition(9, "SPARKLY", null));
    }

    // ---- maintenance ----

    @Test
    void guideIssueReport_createsOpenRecord_andNotifiesOperations() {
        loginAs(7, Roles.GUIDE);
        when(boatRepository.findById(4)).thenReturn(Optional.of(boat));
        when(maintenanceRepository.save(any(BoatMaintenanceRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        IssueReportRequestDTO dto = new IssueReportRequestDTO();
        dto.setBoatId(4);
        dto.setDescription("Engine overheating");

        assertEquals("OPEN", maintenanceService.reportIssue(dto).getMaintenanceStatus());
        verify(notificationService).notifyRole(eq(Roles.OPERATIONS), eq("BOAT_ISSUE"), anyString());
        assertEquals("OPERATIONAL", boat.getStatus()); // a report alone does not take the boat out of service
    }

    @Test
    void operations_cannotDeleteMaintenanceHistory() {
        loginAs(3, Roles.OPERATIONS);

        assertThrows(AccessDeniedException.class, () -> maintenanceService.deleteMaintenanceRecord(1));
    }
}
