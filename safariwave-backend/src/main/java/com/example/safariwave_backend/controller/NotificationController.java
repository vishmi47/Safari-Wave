package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.response.NotificationResponseDTO;
import com.example.safariwave_backend.service.NotificationService;
import com.example.safariwave_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponseDTO>> getForUser(@PathVariable Integer userId,
                                                                      @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                                      Authentication authentication) {
        if (!authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                || a.getAuthority().equals("ROLE_GENERAL_MANAGER"))) {
            if (!authentication.getName().equalsIgnoreCase(userEmail(userId))) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(notificationService.findForUser(userId, unreadOnly));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markRead(@PathVariable Long id, Authentication authentication) {
        Integer userId = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.example.safariwave_backend.exception.ResourceNotFoundException("Authenticated user not found"))
                .getUserId();
        return ResponseEntity.ok(notificationService.markRead(id, userId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        Integer userId = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new com.example.safariwave_backend.exception.ResourceNotFoundException("Authenticated user not found"))
                .getUserId();
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    private String userEmail(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new com.example.safariwave_backend.exception.ResourceNotFoundException("User not found with id: " + userId))
                .getEmail();
    }
}
