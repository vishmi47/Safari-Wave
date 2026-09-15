package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.PartnerVendorEntity;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerVendorRepository extends JpaRepository<PartnerVendorEntity, Long> {

    List<PartnerVendorEntity> findByStatus(PartnerVendorStatus status);

    List<PartnerVendorEntity> findByBusinessNameContainingIgnoreCase(String businessName);

    List<PartnerVendorEntity> findByPartnerTypeContainingIgnoreCase(String partnerType);

    Optional<PartnerVendorEntity> findByEmailIgnoreCase(String email);

    boolean existsByBusinessNameIgnoreCase(String businessName);

    boolean existsByEmailIgnoreCase(String email);

    long countByStatus(PartnerVendorStatus status);
}
