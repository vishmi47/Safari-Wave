package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class PartnerVendorRequestDTO {

    @NotBlank(message = "Business name is required")
    @Size(max = 150, message = "Business name must be under 150 characters")
    private String businessName;

    @NotBlank(message = "Partner type is required")
    @Size(max = 80, message = "Partner type must be under 80 characters")
    private String partnerType;

    @Size(max = 1000, message = "Description must be under 1000 characters")
    private String description;

    @NotBlank(message = "Contact person is required")
    @Size(max = 100, message = "Contact person must be under 100 characters")
    private String contactPerson;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must be under 150 characters")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s0-9]{7,15}$", message = "Phone number format is invalid")
    private String phone;

    @Size(max = 1000, message = "Address must be under 1000 characters")
    private String address;

    @NotBlank(message = "Service type is required")
    @Size(max = 120, message = "Service type must be under 120 characters")
    private String serviceType;

    @Size(max = 2000, message = "Service description must be under 2000 characters")
    private String serviceDescription;

    @Size(max = 80, message = "Contract number must be under 80 characters")
    private String contractNumber;

    @NotNull(message = "Contract start date is required")
    private LocalDate contractStartDate;

    @NotNull(message = "Contract end date is required")
    private LocalDate contractEndDate;

    @Size(max = 2000, message = "Agreement details must be under 2000 characters")
    private String agreementDetails;

    @NotBlank(message = "Document upload is required")
    private String documentUrl;

    public PartnerVendorRequestDTO() {
    }

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
}
