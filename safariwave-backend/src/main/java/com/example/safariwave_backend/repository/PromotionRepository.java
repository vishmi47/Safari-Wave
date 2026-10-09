package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    // Used by the package module to decide between hard delete and archiving
    boolean existsBySafariPackage_PackageId(Integer packageId);

    // Published promotions for a package that are valid on the given date (used to price new bookings)
    @Query("""
            SELECT p FROM Promotion p
            WHERE p.safariPackage.packageId = :packageId
              AND p.status = 'PUBLISHED'
              AND (p.startDate IS NULL OR p.startDate <= :date)
              AND (p.endDate IS NULL OR p.endDate >= :date)
            """)
    List<Promotion> findPublishedForPackageOn(@Param("packageId") Integer packageId, @Param("date") LocalDate date);

    // Paged search. activeOn: only published promotions valid on that date; expiredBefore: only promotions ended before it
    @Query("""
            SELECT p FROM Promotion p
            WHERE (:q IS NULL
                   OR LOWER(p.promotionName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR p.status = :status)
              AND (:activeOn IS NULL OR (p.status = 'PUBLISHED' AND p.startDate <= :activeOn AND p.endDate >= :activeOn))
              AND (:expiredBefore IS NULL OR p.endDate < :expiredBefore)
            """)
    Page<Promotion> search(@Param("q") String q,
                           @Param("status") String status,
                           @Param("activeOn") LocalDate activeOn,
                           @Param("expiredBefore") LocalDate expiredBefore,
                           Pageable pageable);
}
