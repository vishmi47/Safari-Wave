package com.example.safariwave_backend.dto.request;

/**
 * method: CARD (default) or CASH, chosen by PaymentContext.
 * outcome: SUCCESS or FAILED, only for the mock card gateway.
 */
public class PaymentRequestDTO {
    private String method;
    private String outcome;
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
}
