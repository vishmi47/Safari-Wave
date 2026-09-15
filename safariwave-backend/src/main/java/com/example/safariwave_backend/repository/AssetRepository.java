package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Integer> {

    List<Asset> findByAssetType(String assetType);

    List<Asset> findByStatus(String status);

    List<Asset> findByMaintenanceStatus(String maintenanceStatus);
}
