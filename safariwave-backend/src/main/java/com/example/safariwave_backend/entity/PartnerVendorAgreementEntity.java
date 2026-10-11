package com.example.safariwave_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "partner_vendor_agreement", indexes = {
        @Index(name = "idx_partner_vendor_agreement_vendor", columnList = "partner_vendor_id")
})
public class PartnerVendorAgreementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id")
    private Long agreementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_vendor_id", nullable = false)
    private PartnerVendorEntity partnerVendor;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "terms", nullable = false, columnDefinition = "TEXT")
    private String terms;

    @Column(name = "document_url", length = 255)
    private String documentUrl;

    public Long getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(Long agreementId) {
        this.agreementId = agreementId;
    }

    public PartnerVendorEntity getPartnerVendor() {
        return partnerVendor;
    }

    public void setPartnerVendor(PartnerVendorEntity partnerVendor) {
        this.partnerVendor = partnerVendor;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getTerms() {
        return terms;
    }

    public void setTerms(String terms) {
        this.terms = terms;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }
}
