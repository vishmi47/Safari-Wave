package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PaymentRequestDTO {
    @NotBlank private String outcome;
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
}
