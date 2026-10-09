package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Legacy duplicate kept so existing teammate code keeps compiling. Not used by any endpoint.
 * @deprecated use BoatMaintenanceRecordRepository instead (canonical class for the Inventory & Asset module).
 */
@Repository
@Deprecated
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByBoatId(Long boatId);
}