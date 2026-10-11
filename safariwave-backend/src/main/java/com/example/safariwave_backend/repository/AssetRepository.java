package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Integer> {

    List<Asset> findByAssetType(String assetType);

    List<Asset> findByStatus(String status);

    List<Asset> findByMaintenanceStatus(String maintenanceStatus);

    // ---- Inventory & Asset module ----

    boolean existsByBoat_BoatId(Integer boatId);

    boolean existsByBoats_BoatId(Integer boatId);

    // Paged search by name or type, optionally by status
    @Query("""
            SELECT a FROM Asset a
            WHERE (:q IS NULL
                   OR LOWER(a.assetName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.assetType) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:status IS NULL OR a.status = :status)
            """)
    Page<Asset> search(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
