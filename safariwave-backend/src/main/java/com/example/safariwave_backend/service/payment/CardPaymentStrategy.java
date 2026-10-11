package com.example.safariwave_backend.service.payment;

import com.example.safariwave_backend.dto.request.PaymentRequestDTO;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Online card payment through the mock gateway: the request says whether the card is approved or declined.
 * The customer pays their own booking; staff may also record it (checked in PaymentServiceImpl).
 */
@Component
public class CardPaymentStrategy implements PaymentStrategy {

    public static final String METHOD = "CARD";

    @Override
    public boolean supports(String method) {
        return METHOD.equals(method);
    }

    @Override
    public String provider() {
        return "MOCK_CARD";
    }

    @Override
    public PaymentOutcome pay(Booking booking, PaymentRequestDTO request) {
        String outcome = request.getOutcome() == null ? "" : request.getOutcome().trim().toUpperCase(Locale.ROOT);
        return switch (outcome) {
            case "SUCCESS" -> PaymentOutcome.success();
            case "FAILED" -> PaymentOutcome.failed("Mock payment declined");
            default -> throw new BadRequestException("Mock outcome must be SUCCESS or FAILED");
        };
    }
}
