package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.response.NotificationResponseDTO;
import java.util.List;

public interface NotificationService {
    NotificationResponseDTO create(Integer userId, String type, String message);
    List<NotificationResponseDTO> findForUser(Integer userId, boolean unreadOnly);
    NotificationResponseDTO markRead(Long notificationId, Integer userId);
    void markAllRead(Integer userId);

    /** Sends the same notification to every user with the given role (aliases included, e.g. SAFETY for GUIDE). */
    void notifyRole(String roleName, String type, String message);
}
