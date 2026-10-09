package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.response.NotificationResponseDTO;
import com.example.safariwave_backend.entity.Notification;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.NotificationRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public NotificationResponseDTO create(Integer userId, String type, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(type);
        notification.setMessage(message);
        notification.setRead(false);
        return toDto(notificationRepository.save(notification));
    }

    @Override
    public void notifyRole(String roleName, String type, String message) {
        List<String> roleNames = new ArrayList<>(Roles.aliasesOf(roleName));
        roleNames.add(roleName);
        for (User user : userRepository.findByRole_RoleNameInOrderByFirstNameAsc(roleNames)) {
            create(user.getUserId(), type, message);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> findForUser(Integer userId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUser_UserIdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(this::toDto).toList();
    }

    @Override
    public NotificationResponseDTO markRead(Long notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new com.example.safariwave_backend.exception.UnauthorizedException("Notification does not belong to this user");
        }
        notification.setRead(true);
        return toDto(notificationRepository.save(notification));
    }

    @Override
    public void markAllRead(Integer userId) {
        notificationRepository.findByUser_UserIdAndReadFalseOrderByCreatedAtDesc(userId).forEach(notification -> notification.setRead(true));
    }

    private NotificationResponseDTO toDto(Notification notification) {
        return new NotificationResponseDTO(notification.getNotificationId(), notification.getUser().getUserId(),
                notification.getNotificationType(), notification.getMessage(), notification.isRead(), notification.getCreatedAt());
    }
}
