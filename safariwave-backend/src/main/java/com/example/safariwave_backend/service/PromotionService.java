package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.PromotionRequestDTO;
import com.example.safariwave_backend.dto.response.PromotionResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface PromotionService {

    // Create a new promotion (bannerImage: stored banner path, or null)
    PromotionResponseDTO createPromotion(PromotionRequestDTO dto, String bannerImage);

    // Get all promotions (customers only get active ones)
    List<PromotionResponseDTO> getAllPromotions();

    // Get a promotion by ID
    Optional<PromotionResponseDTO> getPromotionById(Integer id);

    // Paged search; status may be DRAFT, PUBLISHED, ACTIVE or EXPIRED
    Page<PromotionResponseDTO> searchPromotions(String query, String status, int page, int size);

    // Update an existing promotion (bannerImage null keeps the current banner)
    PromotionResponseDTO updatePromotion(Integer id, PromotionRequestDTO dto, String bannerImage);

    // Delete a promotion (marketing: drafts and expired promotions only)
    void deletePromotion(Integer id);

    // Publish a promotion
    PromotionResponseDTO publishPromotion(Integer id);

    // Save a promotion as draft
    PromotionResponseDTO draftPromotion(Integer id);
}
