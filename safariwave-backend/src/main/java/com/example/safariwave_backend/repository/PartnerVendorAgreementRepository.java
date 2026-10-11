package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.PartnerVendorAgreementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnerVendorAgreementRepository extends JpaRepository<PartnerVendorAgreementEntity, Long> {

    List<PartnerVendorAgreementEntity> findAllByPartnerVendorPartnerVendorIdOrderByStartDateDescAgreementIdDesc(Long partnerVendorId);

    Optional<PartnerVendorAgreementEntity> findByAgreementIdAndPartnerVendorPartnerVendorId(Long agreementId, Long partnerVendorId);

    Optional<PartnerVendorAgreementEntity> findFirstByDocumentUrl(String documentUrl);

    void deleteAllByPartnerVendorPartnerVendorId(Long partnerVendorId);
}
