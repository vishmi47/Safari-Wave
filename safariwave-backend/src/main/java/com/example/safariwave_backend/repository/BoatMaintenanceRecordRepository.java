package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.BoatMaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoatMaintenanceRecordRepository extends JpaRepository<BoatMaintenanceRecord, Integer> {

    List<BoatMaintenanceRecord> findByBoat(Boat boat);

    List<BoatMaintenanceRecord> findByMaintenanceStatus(String maintenanceStatus);
}
