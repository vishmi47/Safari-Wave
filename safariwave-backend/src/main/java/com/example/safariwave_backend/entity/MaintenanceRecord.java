package com.example.safariwave_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_records")
@Data
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "boat_id", nullable = false)
    private Long boatId;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double cost;

    @Column(name = "technician_name")
    private String technicianName;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;
}