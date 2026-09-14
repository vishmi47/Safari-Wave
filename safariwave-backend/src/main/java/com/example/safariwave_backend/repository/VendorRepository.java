package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    /**
     * Find vendors by specific status (PENDING, APPROVED, etc.)
     */
    List<Vendor> findByStatus(Vendor.VendorStatus status);

    /**
     * Check if email already exists to prevent duplicates
     */
    boolean existsByEmail(String email);
}