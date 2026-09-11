package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.entity.Promotion;
import com.example.safariwave_backend.service.PromotionImageStorageService;
import com.example.safariwave_backend.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin(origins = "*")
public class PromotionController {

    private final PromotionService promotionService;
    private final PromotionImageStorageService imageStorageService;

    // Constructor injection
    public PromotionController(PromotionService promotionService,
                               PromotionImageStorageService imageStorageService) {
        this.promotionService = promotionService;
        this.imageStorageService = imageStorageService;
    }

    // Create a new promotion
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Promotion> createPromotion(
            @RequestPart("promotion") Promotion promotion,
            @RequestPart(value = "banner", required = false) MultipartFile banner) {

        if (banner != null && !banner.isEmpty()) {
            promotion.setBannerImage(imageStorageService.store(banner));
        }

        Promotion createdPromotion =
                promotionService.createPromotion(promotion);

        return new ResponseEntity<>(
                createdPromotion,
                HttpStatus.CREATED
        );
    }

    // Get all promotions
    @GetMapping
    public ResponseEntity<List<Promotion>> getAllPromotions() {

        List<Promotion> promotions =
                promotionService.getAllPromotions();

        return ResponseEntity.ok(promotions);
    }

    // Get promotion by ID
    @GetMapping("/{id}")
    public ResponseEntity<Promotion> getPromotionById(
            @PathVariable Integer id) {

        return ResponseEntity.of(promotionService.getPromotionById(id));
    }

    // Update promotion
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Promotion> updatePromotion(
            @PathVariable Integer id,
            @RequestPart("promotion") Promotion promotion,
            @RequestPart(value = "banner", required = false) MultipartFile banner) {

        if (banner != null && !banner.isEmpty()) {
            promotion.setBannerImage(imageStorageService.store(banner));
        }

        Promotion updatedPromotion =
                promotionService.updatePromotion(id, promotion);

        return ResponseEntity.ok(updatedPromotion);
    }

    // Delete promotion
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(
            @PathVariable Integer id) {

        promotionService.deletePromotion(id);

        return ResponseEntity.noContent().build();
    }

    // Publish promotion
    @PutMapping("/{id}/publish")
    public ResponseEntity<Promotion> publishPromotion(
            @PathVariable Integer id) {

        Promotion promotion =
                promotionService.publishPromotion(id);

        return ResponseEntity.ok(promotion);
    }

    // Move promotion back to draft
    @PutMapping("/{id}/draft")
    public ResponseEntity<Promotion> draftPromotion(
            @PathVariable Integer id) {

        Promotion promotion =
                promotionService.draftPromotion(id);

        return ResponseEntity.ok(promotion);
    }
}