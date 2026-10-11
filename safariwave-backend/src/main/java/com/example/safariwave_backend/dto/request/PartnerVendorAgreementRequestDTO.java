package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class PartnerVendorAgreementRequestDTO {

    @NotNull(message = "Agreement start date is required")
    private LocalDate startDate;

    @NotNull(message = "Agreement end date is required")
    private LocalDate endDate;

    @NotBlank(message = "Agreement terms are required")
    @Size(max = 2000, message = "Agreement terms must be under 2000 characters")
    private String terms;

    @Size(max = 255, message = "Agreement document URL must be under 255 characters")
    private String documentUrl;

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
