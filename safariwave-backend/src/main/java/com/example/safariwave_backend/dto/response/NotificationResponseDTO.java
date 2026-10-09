package com.example.safariwave_backend.dto.response;

import java.time.LocalDateTime;

public record NotificationResponseDTO(Long notificationId, Integer userId, String notificationType,
                                      String message, boolean read, LocalDateTime createdAt) {
}
