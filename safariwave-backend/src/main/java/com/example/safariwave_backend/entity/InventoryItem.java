package com.example.safariwave_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "inventory_items")
@Data
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "min_threshold")
    private Integer minThreshold;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "condition_status")
    private String conditionStatus;

    @Column(name = "last_inspected_date")
    private LocalDate lastInspectedDate;
}