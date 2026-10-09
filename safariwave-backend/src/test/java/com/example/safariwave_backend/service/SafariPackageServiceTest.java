package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.SafariPackageRequestDTO;
import com.example.safariwave_backend.dto.response.SafariPackageResponseDTO;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.PromotionRepository;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.impl.SafariPackageServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SafariPackageServiceTest {

    @Mock
    private SafariPackageRepository safariPackageRepository;

    @Mock
    private SafariTripRepository safariTripRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private SafariPackageServiceImpl safariPackageService;

    private SafariPackageRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new SafariPackageRequestDTO();
        requestDTO.setPackageName("  Sunset Lagoon  ");
        requestDTO.setDescription("Two-hour evening tour");
        requestDTO.setDuration(2);
        requestDTO.setBasePrice(new BigDecimal("4500.00"));
        requestDTO.setMaxPassengers(10);
        requestDTO.setStatus("active");
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

    private SafariPackage existingPackage(String status) {
        SafariPackage pkg = new SafariPackage();
        pkg.setPackageId(5);
        pkg.setPackageName("Mangrove Morning");
        pkg.setBasePrice(new BigDecimal("3000.00"));
        pkg.setStatus(status);
        return pkg;
    }

    // ---- permissions ----

    @Test
    void marketing_canCreatePackage_andCreatedByIsRecorded() {
        loginAs(3, Roles.MARKETING);
        when(safariPackageRepository.save(any(SafariPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SafariPackageResponseDTO response = safariPackageService.createPackage(requestDTO);

        assertEquals("Sunset Lagoon", response.getPackageName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(3, response.getCreatedBy());
    }

    @Test
    void customer_cannotCreatePackage() {
        loginAs(10, Roles.CUSTOMER);

        assertThrows(AccessDeniedException.class, () -> safariPackageService.createPackage(requestDTO));
        verify(safariPackageRepository, never()).save(any());
    }

    @Test
    void guide_hasNoReadAccess() {
        loginAs(7, Roles.GUIDE);

        assertThrows(AccessDeniedException.class, () -> safariPackageService.getAllPackages());
    }

    @Test
    void customer_cannotSeeInactivePackage() {
        loginAs(10, Roles.CUSTOMER);
        when(safariPackageRepository.findById(5)).thenReturn(Optional.of(existingPackage("INACTIVE")));

        assertTrue(safariPackageService.getPackageById(5).isEmpty());
    }

    @Test
    void customer_searchIsAlwaysLimitedToActivePackages() {
        loginAs(10, Roles.CUSTOMER);
        when(safariPackageRepository.search(isNull(), eq("ACTIVE"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        safariPackageService.searchPackages(null, "INACTIVE", 0, 10);

        verify(safariPackageRepository).search(isNull(), eq("ACTIVE"), any(Pageable.class));
    }

    // ---- validation ----

    @Test
    void invalidStatus_shouldBeRejected() {
        loginAs(3, Roles.MARKETING);
        requestDTO.setStatus("ARCHIVED");

        assertThrows(BadRequestException.class, () -> safariPackageService.createPackage(requestDTO));
        verify(safariPackageRepository, never()).save(any());
    }

    @Test
    void search_withInvalidPageSize_shouldBeRejected() {
        loginAs(3, Roles.MARKETING);

        assertThrows(BadRequestException.class, () -> safariPackageService.searchPackages(null, null, 0, 0));
    }

    // ---- delete rules ----

    @Test
    void marketing_cannotDeleteActivePackage() {
        loginAs(3, Roles.MARKETING);
        when(safariPackageRepository.findById(5)).thenReturn(Optional.of(existingPackage("ACTIVE")));

        assertThrows(BadRequestException.class, () -> safariPackageService.deletePackage(5));
        verify(safariPackageRepository, never()).delete(any());
    }

    @Test
    void deletingReferencedPackage_shouldArchiveInsteadOfDelete() {
        loginAs(3, Roles.MARKETING);
        SafariPackage pkg = existingPackage("INACTIVE");
        when(safariPackageRepository.findById(5)).thenReturn(Optional.of(pkg));
        when(safariTripRepository.existsBySafariPackage_PackageId(5)).thenReturn(true);

        safariPackageService.deletePackage(5);

        assertEquals("ARCHIVED", pkg.getStatus());
        verify(safariPackageRepository).save(pkg);
        verify(safariPackageRepository, never()).delete(any());
    }

    @Test
    void deletingUnusedPackage_shouldHardDelete() {
        loginAs(3, Roles.MARKETING);
        SafariPackage pkg = existingPackage("INACTIVE");
        when(safariPackageRepository.findById(5)).thenReturn(Optional.of(pkg));
        when(safariTripRepository.existsBySafariPackage_PackageId(5)).thenReturn(false);
        when(bookingRepository.existsBySafariPackage_PackageId(5)).thenReturn(false);
        when(promotionRepository.existsBySafariPackage_PackageId(5)).thenReturn(false);

        safariPackageService.deletePackage(5);

        verify(safariPackageRepository).delete(pkg);
    }

    @Test
    void admin_canDeleteActivePackage() {
        loginAs(2, "GENERAL_MANAGER");
        SafariPackage pkg = existingPackage("ACTIVE");
        when(safariPackageRepository.findById(5)).thenReturn(Optional.of(pkg));
        when(safariTripRepository.existsBySafariPackage_PackageId(5)).thenReturn(true);

        safariPackageService.deletePackage(5);

        assertEquals("ARCHIVED", pkg.getStatus());
    }
}
