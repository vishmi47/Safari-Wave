package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.WeatherAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface WeatherAlertRepository extends JpaRepository<WeatherAlert, Integer> {

    List<WeatherAlert> findDistinctByAffectedIncidents_IncidentId(Integer incidentId);

    List<WeatherAlert> findBySeverity(String severity);

    List<WeatherAlert> findByAlertStatus(String alertStatus);

    // Paged search by location, condition or description, optionally by status
    @Query("""
            SELECT a FROM WeatherAlert a
            WHERE (:q IS NULL
                   OR LOWER(a.location) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.weatherCondition) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR a.alertStatus = :status)
            """)
    Page<WeatherAlert> search(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
