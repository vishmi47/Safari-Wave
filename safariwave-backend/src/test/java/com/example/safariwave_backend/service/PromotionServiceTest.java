package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.PromotionRequestDTO;
import com.example.safariwave_backend.dto.response.PromotionResponseDTO;
import com.example.safariwave_backend.entity.Promotion;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.repository.PromotionRepository;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.security.AuthenticatedUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.impl.PromotionServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Promotions (spec 6.3 / B.3). */
@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private SafariPackageRepository safariPackageRepository;

    @InjectMocks
    private PromotionServiceImpl promotionService;

    private SafariPackage safariPackage;

    @BeforeEach
    void setUp() {
        safariPackage = new SafariPackage();
        safariPackage.setPackageId(1);
        safariPackage.setPackageName("Sunset Lagoon");
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

    private PromotionRequestDTO request(LocalDate start, LocalDate end) {
        PromotionRequestDTO dto = new PromotionRequestDTO();
        dto.setPromotionName("Monsoon Saver");
        dto.setDiscountPercentage(new BigDecimal("15"));
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setPackageId(1);
        return dto;
    }

    private Promotion promotion(String status, LocalDate start, LocalDate end) {
        Promotion promotion = new Promotion();
        promotion.setPromotionId(3);
        promotion.setPromotionName("Monsoon Saver");
        promotion.setDiscountPercentage(new BigDecimal("15"));
        promotion.setStartDate(start);
        promotion.setEndDate(end);
        promotion.setStatus(status);
        promotion.setSafariPackage(safariPackage);
        return promotion;
    }

    @Test
    void marketing_createsDraftPromotion() {
        loginAs(3, Roles.MARKETING);
        when(safariPackageRepository.findById(1)).thenReturn(Optional.of(safariPackage));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PromotionResponseDTO response = promotionService.createPromotion(request(LocalDate.now(), LocalDate.now().plusDays(30)), null);

        assertEquals("DRAFT", response.getStatus());
        assertEquals("Sunset Lagoon", response.getPackageName());
        assertEquals(3, response.getCreatedBy());
    }

    @Test
    void finance_cannotCreatePromotion() {
        loginAs(6, Roles.FINANCE);

        assertThrows(AccessDeniedException.class,
                () -> promotionService.createPromotion(request(LocalDate.now(), LocalDate.now().plusDays(5)), null));
    }

    @Test
    void startAfterEnd_isRejected() {
        loginAs(3, Roles.MARKETING);

        assertThrows(BadRequestException.class,
                () -> promotionService.createPromotion(request(LocalDate.now().plusDays(10), LocalDate.now()), null));
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void customer_onlySeesActivePromotions() {
        loginAs(10, Roles.CUSTOMER);
        LocalDate today = LocalDate.now();
        when(promotionRepository.findAll()).thenReturn(List.of(
                promotion("PUBLISHED", today.minusDays(1), today.plusDays(5)),   // active
                promotion("DRAFT", today.minusDays(1), today.plusDays(5)),       // not published
                promotion("PUBLISHED", today.plusDays(3), today.plusDays(9)),    // not started
                promotion("PUBLISHED", today.minusDays(9), today.minusDays(2))));// expired

        List<PromotionResponseDTO> visible = promotionService.getAllPromotions();

        assertEquals(1, visible.size());
        assertTrue(visible.get(0).isActive());
    }

    @Test
    void guide_hasNoAccess() {
        loginAs(7, Roles.GUIDE);

        assertThrows(AccessDeniedException.class, () -> promotionService.getAllPromotions());
    }

    @Test
    void marketing_cannotDeleteRunningPublishedPromotion_butCanDeleteExpiredOne() {
        loginAs(3, Roles.MARKETING);
        LocalDate today = LocalDate.now();
        Promotion running = promotion("PUBLISHED", today.minusDays(1), today.plusDays(5));
        when(promotionRepository.findById(3)).thenReturn(Optional.of(running));
        assertThrows(BadRequestException.class, () -> promotionService.deletePromotion(3));

        Promotion expired = promotion("PUBLISHED", today.minusDays(9), today.minusDays(2));
        when(promotionRepository.findById(3)).thenReturn(Optional.of(expired));
        promotionService.deletePromotion(3);
        verify(promotionRepository).delete(expired);
    }

    @Test
    void expiredPromotion_cannotBePublished() {
        loginAs(3, Roles.MARKETING);
        when(promotionRepository.findById(3)).thenReturn(Optional.of(promotion("DRAFT", LocalDate.now().minusDays(9), LocalDate.now().minusDays(1))));

        assertThrows(BadRequestException.class, () -> promotionService.publishPromotion(3));
    }
}
