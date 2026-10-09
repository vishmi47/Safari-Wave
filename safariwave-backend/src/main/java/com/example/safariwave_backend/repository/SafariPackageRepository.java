package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.SafariPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface SafariPackageRepository extends JpaRepository<SafariPackage, Integer> {

    List<SafariPackage> findByStatus(String status);

    List<SafariPackage> findByStatusIsNullOrStatusNot(String status);

    // Paged search by name/description. With no status filter, archived (soft-deleted) packages are hidden.
    @Query("""
            SELECT p FROM SafariPackage p
            WHERE (:q IS NULL
                   OR LOWER(p.packageName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND ((:status IS NULL AND (p.status IS NULL OR p.status <> 'ARCHIVED'))
                   OR p.status = :status)
            """)
    Page<SafariPackage> search(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
