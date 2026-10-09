package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.PartnerVendorEntity;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;

@Repository
public interface PartnerVendorRepository extends JpaRepository<PartnerVendorEntity, Long> {

    List<PartnerVendorEntity> findByStatus(PartnerVendorStatus status);

    List<PartnerVendorEntity> findByBusinessNameContainingIgnoreCase(String businessName);

    List<PartnerVendorEntity> findByPartnerTypeContainingIgnoreCase(String partnerType);

    Optional<PartnerVendorEntity> findByEmailIgnoreCase(String email);

    boolean existsByBusinessNameIgnoreCase(String businessName);

    boolean existsByEmailIgnoreCase(String email);

    long countByStatus(PartnerVendorStatus status);

    Optional<PartnerVendorEntity> findFirstByDocumentUrl(String documentUrl);

    // Paged search; types limits results to the partner types the role may see (spec B.3)
    @Query("""
            SELECT p FROM PartnerVendorEntity p
            WHERE (:allTypes = true OR UPPER(p.partnerType) IN :types)
              AND (:status IS NULL OR p.status = :status)
              AND (:q IS NULL
                   OR LOWER(p.businessName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.contactPerson) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.serviceType) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<PartnerVendorEntity> search(@Param("q") String q,
                                     @Param("status") PartnerVendorStatus status,
                                     @Param("allTypes") boolean allTypes,
                                     @Param("types") Collection<String> types,
                                     Pageable pageable);
}
