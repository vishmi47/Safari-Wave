package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.response.LoyaltyResponseDTO;
import com.example.safariwave_backend.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {
    private final LoyaltyService loyaltyService;
    public LoyaltyController(LoyaltyService loyaltyService) { this.loyaltyService = loyaltyService; }
    @PostMapping("/customers/{customerId}/enroll")
    public ResponseEntity<LoyaltyResponseDTO> enroll(@PathVariable Integer customerId) { return ResponseEntity.ok(loyaltyService.enroll(customerId)); }
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<LoyaltyResponseDTO> get(@PathVariable Integer customerId) { return ResponseEntity.ok(loyaltyService.getByCustomer(customerId)); }
    @PatchMapping("/customers/{customerId}/status")
    public ResponseEntity<LoyaltyResponseDTO> status(@PathVariable Integer customerId, @RequestParam String value) { return ResponseEntity.ok(loyaltyService.updateStatus(customerId, value)); }
    @PostMapping("/customers/{customerId}/points")
    public ResponseEntity<LoyaltyResponseDTO> points(@PathVariable Integer customerId, @RequestParam long value) { return ResponseEntity.ok(loyaltyService.addPoints(customerId, value)); }
}
