package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Boat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BoatRepository extends JpaRepository<Boat, Integer> {

    // ---- Inventory & Asset module ----

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    boolean existsByRegistrationNumberIgnoreCaseAndBoatIdNot(String registrationNumber, Integer boatId);

    // Paged search by name or registration number, optionally by status
    @Query("""
            SELECT b FROM Boat b
            WHERE (:q IS NULL
                   OR LOWER(b.boatName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(b.registrationNumber) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR b.status = :status)
            """)
    Page<Boat> search(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
