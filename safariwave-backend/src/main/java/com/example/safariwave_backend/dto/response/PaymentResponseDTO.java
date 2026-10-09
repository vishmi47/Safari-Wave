package com.example.safariwave_backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDTO(Long paymentId, Integer bookingId, BigDecimal amount, String status,
                                 String provider, String failureReason, LocalDateTime createdAt) {}
