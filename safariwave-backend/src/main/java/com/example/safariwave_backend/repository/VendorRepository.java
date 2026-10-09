package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Legacy duplicate kept so existing teammate code keeps compiling.
 * @deprecated use PartnerVendorRepository instead (canonical class for the Partner & Vendor module).
 */
@Repository
@Deprecated
public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByStatus(Vendor.VendorStatus status);
    boolean existsByEmail(String email);
}