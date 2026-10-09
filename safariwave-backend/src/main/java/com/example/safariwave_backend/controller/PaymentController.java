package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.PaymentRequestDTO;
import com.example.safariwave_backend.dto.response.PaymentResponseDTO;
import com.example.safariwave_backend.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) { this.paymentService = paymentService; }
    @PostMapping("/bookings/{bookingId}/mock")
    public ResponseEntity<PaymentResponseDTO> process(@PathVariable Integer bookingId, @Valid @RequestBody PaymentRequestDTO request) { return ResponseEntity.ok(paymentService.processMockPayment(bookingId, request)); }
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<PaymentResponseDTO> get(@PathVariable Integer bookingId) { return ResponseEntity.ok(paymentService.getByBooking(bookingId)); }
}
