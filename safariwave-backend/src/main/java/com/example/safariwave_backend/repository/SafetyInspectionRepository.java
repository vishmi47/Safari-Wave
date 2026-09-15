package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.SafetyInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafetyInspectionRepository extends JpaRepository<SafetyInspection, Integer> {

    List<SafetyInspection> findByBoat(Boat boat);

    List<SafetyInspection> findBySafetyStatus(String safetyStatus);
}
