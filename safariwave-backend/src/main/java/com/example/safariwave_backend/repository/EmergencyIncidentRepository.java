package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.EmergencyIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyIncidentRepository extends JpaRepository<EmergencyIncident, Integer> {

    List<EmergencyIncident> findBySeverity(String severity);

    List<EmergencyIncident> findByStatus(String status);
}
