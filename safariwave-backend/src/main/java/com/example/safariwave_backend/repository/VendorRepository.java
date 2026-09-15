package com.example.safariwave_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByStatus(Vendor.VendorStatus status);
    boolean existsByEmail(String email);
}