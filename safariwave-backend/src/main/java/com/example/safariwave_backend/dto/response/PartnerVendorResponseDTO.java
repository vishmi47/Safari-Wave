package com.example.safariwave_backend.dto.response;

import com.example.safariwave_backend.enums.PartnerVendorStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PartnerVendorResponseDTO {

    private Long partnerVendorId;
    private String businessName;
    private String partnerType;
    private String description;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String serviceType;
    private String serviceDescription;
    private String contractNumber;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String agreementDetails;
    private String documentUrl;
    private PartnerVendorStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PartnerVendorResponseDTO() {
    }

    public Long getPartnerVendorId() { return partnerVendorId; }
    public void setPartnerVendorId(Long partnerVendorId) { this.partnerVendorId = partnerVendorId; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public LocalDate getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(LocalDate contractStartDate) { this.contractStartDate = contractStartDate; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    public String getAgreementDetails() { return agreementDetails; }
    public void setAgreementDetails(String agreementDetails) { this.agreementDetails = agreementDetails; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public PartnerVendorStatus getStatus() { return status; }
    public void setStatus(PartnerVendorStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
