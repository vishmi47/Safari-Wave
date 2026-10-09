package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
    List<Notification> findByUser_UserIdAndReadFalseOrderByCreatedAtDesc(Integer userId);
}
