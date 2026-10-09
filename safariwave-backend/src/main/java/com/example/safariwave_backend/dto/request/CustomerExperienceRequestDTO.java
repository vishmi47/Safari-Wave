package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerExperienceRequestDTO {

    // Customers always submit for themselves; this is only used by ADMIN
    private Integer customerId;
    private Integer bookingId;
    @NotBlank
    private String experienceType;
    @NotBlank
    @Size(max = 150)
    private String subject;
    @NotBlank
    @Size(max = 4000)
    private String description;
    // 1-5; only for a completed trip of the customer's own booking
    @Min(1)
    @Max(5)
    private Integer rating;
    private String status;
    @Size(max = 4000)
    private String response;
    @Size(max = 4000)
    private String internalNotes;

    public CustomerExperienceRequestDTO() {
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }

    public String getExperienceType() {
        return experienceType;
    }

    public void setExperienceType(String experienceType) {
        this.experienceType = experienceType;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }
}
