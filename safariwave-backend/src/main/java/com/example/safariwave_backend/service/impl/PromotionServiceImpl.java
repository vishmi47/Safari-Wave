package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.PromotionRequestDTO;
import com.example.safariwave_backend.dto.response.PromotionResponseDTO;
import com.example.safariwave_backend.entity.Promotion;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.PromotionRepository;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.PromotionService;
import com.example.safariwave_backend.service.support.PagingSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Promotions - Promotion & Safari Package module (spec 6.3 / B.3).
 *
 * MARKETING: create, read, update, publish/draft, delete drafts and expired promotions.
 * CUSTOMER_RELATIONS, FINANCE, OPERATIONS: read all. CUSTOMER: published promotions valid today only.
 * GUIDE: no access. ADMIN: everything.
 */
@Service
public class PromotionServiceImpl implements PromotionService {

    static final String DRAFT = "DRAFT";
    static final String PUBLISHED = "PUBLISHED";
    private static final List<String> SEARCH_STATUSES = List.of(DRAFT, PUBLISHED, "ACTIVE", "EXPIRED");

    private final PromotionRepository promotionRepository;
    private final SafariPackageRepository safariPackageRepository;

    // Constructor injection
    public PromotionServiceImpl(PromotionRepository promotionRepository, SafariPackageRepository safariPackageRepository) {
        this.promotionRepository = promotionRepository;
        this.safariPackageRepository = safariPackageRepository;
    }

    // Create a new promotion
    @Override
    public PromotionResponseDTO createPromotion(PromotionRequestDTO dto, String bannerImage) {
        CurrentUser.requireAnyRole(Roles.MARKETING);
        Promotion promotion = new Promotion();
        copyRequestToEntity(dto, promotion);
        promotion.setStatus(requireStatus(dto.getStatus(), DRAFT));
        promotion.setBannerImage(bannerImage);
        promotion.setCreatedBy(CurrentUser.id());
        requirePublishable(promotion);
        return toResponseDTO(promotionRepository.save(promotion));
    }

    // Get all promotions
    @Override
    public List<PromotionResponseDTO> getAllPromotions() {
        requireReadAccess();
        return promotionRepository.findAll().stream()
                .filter(promotion -> !isCustomer() || isActive(promotion))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Get promotion by ID
    @Override
    public Optional<PromotionResponseDTO> getPromotionById(Integer id) {
        requireReadAccess();
        return promotionRepository.findById(id)
                .filter(promotion -> !isCustomer() || isActive(promotion))
                .map(this::toResponseDTO);
    }

    @Override
    public Page<PromotionResponseDTO> searchPromotions(String query, String status, int page, int size) {
        requireReadAccess();
        String filter = isCustomer() ? "ACTIVE" : PagingSupport.status(status, SEARCH_STATUSES);
        LocalDate today = LocalDate.now();
        String storedStatus = DRAFT.equals(filter) || PUBLISHED.equals(filter) ? filter : null;
        LocalDate activeOn = "ACTIVE".equals(filter) ? today : null;
        LocalDate expiredBefore = "EXPIRED".equals(filter) ? today : null;
        return promotionRepository.search(PagingSupport.text(query), storedStatus, activeOn, expiredBefore,
                        PagingSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "startDate")))
                .map(this::toResponseDTO);
    }

    // Update an existing promotion
    @Override
    public PromotionResponseDTO updatePromotion(Integer id, PromotionRequestDTO dto, String bannerImage) {
        CurrentUser.requireAnyRole(Roles.MARKETING);
        Promotion existingPromotion = findPromotion(id);
        copyRequestToEntity(dto, existingPromotion);
        existingPromotion.setStatus(requireStatus(dto.getStatus(), existingPromotion.getStatus()));
        if (bannerImage != null) {
            existingPromotion.setBannerImage(bannerImage);
        }
        requirePublishable(existingPromotion);
        // Bookings keep the discount they were made with, so editing a promotion never changes past bookings
        return toResponseDTO(promotionRepository.save(existingPromotion));
    }

    // Delete a promotion
    @Override
    public void deletePromotion(Integer id) {
        CurrentUser.requireAnyRole(Roles.MARKETING);
        Promotion promotion = findPromotion(id);
        // Spec B.3: marketing removes expired or inactive items only; ADMIN may delete any promotion
        if (!CurrentUser.isAdmin() && PUBLISHED.equals(promotion.getStatus()) && !isExpired(promotion)) {
            throw new BadRequestException("Only draft or expired promotions can be deleted. Move it back to draft first.");
        }
        promotionRepository.delete(promotion);
    }

    // Publish a promotion
    @Override
    public PromotionResponseDTO publishPromotion(Integer id) {
        CurrentUser.requireAnyRole(Roles.MARKETING);
        Promotion promotion = findPromotion(id);
        promotion.setStatus(PUBLISHED);
        requirePublishable(promotion);
        return toResponseDTO(promotionRepository.save(promotion));
    }

    // Save a promotion as draft
    @Override
    public PromotionResponseDTO draftPromotion(Integer id) {
        CurrentUser.requireAnyRole(Roles.MARKETING);
        Promotion promotion = findPromotion(id);
        promotion.setStatus(DRAFT);
        return toResponseDTO(promotionRepository.save(promotion));
    }

    private void copyRequestToEntity(PromotionRequestDTO dto, Promotion promotion) {
        // Validate date range (spec B.3: validFrom must be on or before validTo)
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new BadRequestException("Start date cannot be after end date.");
        }
        SafariPackage safariPackage = safariPackageRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + dto.getPackageId()));
        promotion.setPromotionName(dto.getPromotionName().trim());
        promotion.setDescription(dto.getDescription());
        promotion.setDiscountPercentage(dto.getDiscountPercentage());
        promotion.setStartDate(dto.getStartDate());
        promotion.setEndDate(dto.getEndDate());
        promotion.setSafariPackage(safariPackage);
    }

    private void requirePublishable(Promotion promotion) {
        if (PUBLISHED.equals(promotion.getStatus()) && isExpired(promotion)) {
            throw new BadRequestException("This promotion has already ended and cannot be published.");
        }
    }

    private String requireStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!DRAFT.equals(normalized) && !PUBLISHED.equals(normalized)) {
            throw new BadRequestException("Status must be DRAFT or PUBLISHED.");
        }
        return normalized;
    }

    private void requireReadAccess() {
        CurrentUser.requireAnyRole(Roles.MARKETING, Roles.CUSTOMER_RELATIONS, Roles.FINANCE, Roles.OPERATIONS, Roles.CUSTOMER);
    }

    private boolean isCustomer() {
        return CurrentUser.hasAnyRole(Roles.CUSTOMER);
    }

    private boolean isActive(Promotion promotion) {
        LocalDate today = LocalDate.now();
        return PUBLISHED.equals(promotion.getStatus())
                && !promotion.getStartDate().isAfter(today) && !promotion.getEndDate().isBefore(today);
    }

    private boolean isExpired(Promotion promotion) {
        return promotion.getEndDate() != null && promotion.getEndDate().isBefore(LocalDate.now());
    }

    private Promotion findPromotion(Integer id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with ID: " + id));
    }

    private PromotionResponseDTO toResponseDTO(Promotion promotion) {
        PromotionResponseDTO dto = new PromotionResponseDTO();
        dto.setPromotionId(promotion.getPromotionId());
        dto.setPromotionName(promotion.getPromotionName());
        dto.setDescription(promotion.getDescription());
        dto.setDiscountPercentage(promotion.getDiscountPercentage());
        dto.setStartDate(promotion.getStartDate());
        dto.setEndDate(promotion.getEndDate());
        dto.setStatus(promotion.getStatus());
        dto.setBannerImage(promotion.getBannerImage());
        if (promotion.getSafariPackage() != null) {
            dto.setPackageId(promotion.getSafariPackage().getPackageId());
            dto.setPackageName(promotion.getSafariPackage().getPackageName());
        }
        dto.setActive(isActive(promotion));
        dto.setExpired(isExpired(promotion));
        dto.setCreatedAt(promotion.getCreatedAt());
        dto.setUpdatedAt(promotion.getUpdatedAt());
        dto.setCreatedBy(promotion.getCreatedBy());
        return dto;
    }
}
