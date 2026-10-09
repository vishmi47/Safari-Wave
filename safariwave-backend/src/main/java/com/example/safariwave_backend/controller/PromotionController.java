package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.PromotionRequestDTO;
import com.example.safariwave_backend.dto.response.PromotionResponseDTO;
import com.example.safariwave_backend.service.PromotionImageStorageService;
import com.example.safariwave_backend.service.PromotionService;
import jakarta.validation.Valid;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Role rules for promotions are enforced in PromotionServiceImpl.
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;
    private final PromotionImageStorageService imageStorageService;

    // Constructor injection
    public PromotionController(PromotionService promotionService,
                               PromotionImageStorageService imageStorageService) {
        this.promotionService = promotionService;
        this.imageStorageService = imageStorageService;
    }

    // Create a new promotion (multipart: "promotion" JSON part + optional "banner" image)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PromotionResponseDTO> createPromotion(
            @Valid @RequestPart("promotion") PromotionRequestDTO promotion,
            @RequestPart(value = "banner", required = false) MultipartFile banner) {
        return new ResponseEntity<>(promotionService.createPromotion(promotion, storeBanner(banner)), HttpStatus.CREATED);
    }

    // Create a new promotion without a banner (plain JSON)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PromotionResponseDTO> createPromotionJson(@Valid @RequestBody PromotionRequestDTO promotion) {
        return new ResponseEntity<>(promotionService.createPromotion(promotion, null), HttpStatus.CREATED);
    }

    // Get all promotions
    @GetMapping
    public ResponseEntity<List<PromotionResponseDTO>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    // Paged list with search: GET /api/promotions/search?q=&status=ACTIVE&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<PagedModel<PromotionResponseDTO>> searchPromotions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(promotionService.searchPromotions(q, status, page, size)));
    }

    // Get promotion by ID
    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponseDTO> getPromotionById(@PathVariable Integer id) {
        return ResponseEntity.of(promotionService.getPromotionById(id));
    }

    // Update promotion (multipart; a new banner replaces the old one)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PromotionResponseDTO> updatePromotion(
            @PathVariable Integer id,
            @Valid @RequestPart("promotion") PromotionRequestDTO promotion,
            @RequestPart(value = "banner", required = false) MultipartFile banner) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, promotion, storeBanner(banner)));
    }

    // Update promotion (plain JSON, banner unchanged)
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PromotionResponseDTO> updatePromotionJson(@PathVariable Integer id,
                                                                    @Valid @RequestBody PromotionRequestDTO promotion) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, promotion, null));
    }

    // Delete promotion
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Integer id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    // Publish promotion
    @PutMapping("/{id}/publish")
    public ResponseEntity<PromotionResponseDTO> publishPromotion(@PathVariable Integer id) {
        return ResponseEntity.ok(promotionService.publishPromotion(id));
    }

    // Move promotion back to draft
    @PutMapping("/{id}/draft")
    public ResponseEntity<PromotionResponseDTO> draftPromotion(@PathVariable Integer id) {
        return ResponseEntity.ok(promotionService.draftPromotion(id));
    }

    private String storeBanner(MultipartFile banner) {
        return banner != null && !banner.isEmpty() ? imageStorageService.store(banner) : null;
    }
}
