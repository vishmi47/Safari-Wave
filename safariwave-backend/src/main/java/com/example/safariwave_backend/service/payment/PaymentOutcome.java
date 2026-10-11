package com.example.safariwave_backend.service.payment;

/** Result of a PaymentStrategy: SUCCESS or FAILED, plus the reason when it failed. */
public record PaymentOutcome(String status, String failureReason) {

    public static PaymentOutcome success() {
        return new PaymentOutcome("SUCCESS", null);
    }

    public static PaymentOutcome failed(String reason) {
        return new PaymentOutcome("FAILED", reason);
    }

    public boolean succeeded() {
        return "SUCCESS".equals(status);
    }
}
