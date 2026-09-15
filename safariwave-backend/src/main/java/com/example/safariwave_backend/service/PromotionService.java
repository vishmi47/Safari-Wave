package com.example.safariwave_backend.service;

import com.example.safariwave_backend.entity.Promotion;

import java.util.List;
import java.util.Optional;

public interface PromotionService {

    // Create a new promotion
    Promotion createPromotion(Promotion promotion);

    // Get all promotions
    List<Promotion> getAllPromotions();

    // Get a promotion by ID
    Optional<Promotion> getPromotionById(Integer id);

    // Update an existing promotion
    Promotion updatePromotion(Integer id, Promotion promotion);

    // Delete a promotion
    void deletePromotion(Integer id);

    // Publish a promotion
    Promotion publishPromotion(Integer id);

    // Save a promotion as draft
    Promotion draftPromotion(Integer id);
}