package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.EmergencyIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface EmergencyIncidentRepository extends JpaRepository<EmergencyIncident, Integer> {

    List<EmergencyIncident> findBySeverity(String severity);

    List<EmergencyIncident> findByStatus(String status);

    // Used by the booking module before a trip may be deleted
    boolean existsBySafariTrip_TripId(Integer tripId);

    // Paged search by type or description, optionally by status
    @Query("""
            SELECT e FROM EmergencyIncident e
            WHERE (:q IS NULL
                   OR LOWER(e.incidentType) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR e.status = :status)
            """)
    Page<EmergencyIncident> search(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
