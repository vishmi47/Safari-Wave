package com.example.safariwave_backend.dto.response;

import java.time.LocalDateTime;

public record LoyaltyResponseDTO(Integer loyaltyId, Integer customerId, String status, long points,
                                 LocalDateTime enrolledAt, LocalDateTime updatedAt) {}
