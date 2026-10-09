package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.SafetyInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SafetyInspectionRepository extends JpaRepository<SafetyInspection, Integer> {

    List<SafetyInspection> findByBoat(Boat boat);

    List<SafetyInspection> findBySafetyStatus(String safetyStatus);

    // Used by the booking module before a trip may be deleted
    boolean existsBySafariTrip_TripId(Integer tripId);

    boolean existsByBoat_BoatId(Integer boatId);

    // Paged search by findings, inspector or boat name, optionally by safety status
    @Query("""
            SELECT i FROM SafetyInspection i LEFT JOIN i.boat b
            WHERE (:q IS NULL
                   OR LOWER(i.findings) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(i.inspector) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(b.boatName) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR i.safetyStatus = :status)
            """)
    Page<SafetyInspection> search(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
