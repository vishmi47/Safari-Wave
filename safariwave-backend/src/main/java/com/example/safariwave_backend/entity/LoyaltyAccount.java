package com.example.safariwave_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_account", uniqueConstraints = @UniqueConstraint(name = "uk_loyalty_customer", columnNames = "customer_id"))
public class LoyaltyAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer loyaltyId;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(nullable = false)
    private long points;
    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void onCreate() { enrolledAt = LocalDateTime.now(); updatedAt = enrolledAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Integer getLoyaltyId() { return loyaltyId; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
