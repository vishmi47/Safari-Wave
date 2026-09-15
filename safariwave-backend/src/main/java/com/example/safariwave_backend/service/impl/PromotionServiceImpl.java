package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.entity.Promotion;
import com.example.safariwave_backend.repository.PromotionRepository;
import com.example.safariwave_backend.service.PromotionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    // Constructor injection
    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    // Create a new promotion
    @Override
    public Promotion createPromotion(Promotion promotion) {

        // Validate promotion name
        if (promotion.getPromotionName() == null ||
                promotion.getPromotionName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Promotion name cannot be empty"
            );
        }

        // Validate discount
        if (promotion.getDiscountPercentage() == null ||
                promotion.getDiscountPercentage().compareTo(BigDecimal.ZERO) < 0 ||
                promotion.getDiscountPercentage().compareTo(new BigDecimal("100")) > 0) {

            throw new IllegalArgumentException(
                    "Discount percentage must be between 0 and 100"
            );
        }

        // Validate date range
        if (promotion.getStartDate() != null &&
                promotion.getEndDate() != null &&
                promotion.getStartDate().isAfter(promotion.getEndDate())) {

            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        return promotionRepository.save(promotion);
    }


    // Get all promotions
    @Override
    public List<Promotion> getAllPromotions() {

        return promotionRepository.findAll();
    }


    // Get promotion by ID
    @Override
    public Optional<Promotion> getPromotionById(Integer id) {

        return promotionRepository.findById(id);
    }


    // Update an existing promotion
    @Override
    public Promotion updatePromotion(Integer id, Promotion promotion) {

        // Check whether promotion exists
        Promotion existingPromotion = promotionRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Promotion not found with ID: " + id
                        )
                );

        // Validate promotion name
        if (promotion.getPromotionName() == null ||
                promotion.getPromotionName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Promotion name cannot be empty"
            );
        }

        // Validate discount
        if (promotion.getDiscountPercentage() == null ||
                promotion.getDiscountPercentage().compareTo(BigDecimal.ZERO) < 0 ||
                promotion.getDiscountPercentage().compareTo(new BigDecimal("100")) > 0) {

            throw new IllegalArgumentException(
                    "Discount percentage must be between 0 and 100"
            );
        }

        // Validate date range
        if (promotion.getStartDate() != null &&
                promotion.getEndDate() != null &&
                promotion.getStartDate().isAfter(promotion.getEndDate())) {

            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        // Update fields
        existingPromotion.setPromotionName(
                promotion.getPromotionName()
        );

        existingPromotion.setDescription(
                promotion.getDescription()
        );

        existingPromotion.setDiscountPercentage(
                promotion.getDiscountPercentage()
        );

        existingPromotion.setStartDate(
                promotion.getStartDate()
        );

        existingPromotion.setEndDate(
                promotion.getEndDate()
        );

        existingPromotion.setStatus(
                promotion.getStatus()
        );

        existingPromotion.setBannerImage(
                promotion.getBannerImage()
        );

        // Save updated promotion
        return promotionRepository.save(existingPromotion);
    }


    // Delete a promotion
    @Override
    public void deletePromotion(Integer id) {

        // Check whether promotion exists
        if (!promotionRepository.existsById(id)) {

            throw new RuntimeException(
                    "Promotion not found with ID: " + id
            );
        }

        promotionRepository.deleteById(id);
    }


    // Publish a promotion
    @Override
    public Promotion publishPromotion(Integer id) {

        Promotion promotion = promotionRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Promotion not found with ID: " + id
                        )
                );

        promotion.setStatus("PUBLISHED");

        return promotionRepository.save(promotion);
    }


    // Save a promotion as draft
    @Override
    public Promotion draftPromotion(Integer id) {

        Promotion promotion = promotionRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Promotion not found with ID: " + id
                        )
                );

        promotion.setStatus("DRAFT");

        return promotionRepository.save(promotion);
    }
}