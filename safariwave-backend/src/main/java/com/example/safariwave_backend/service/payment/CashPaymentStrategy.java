package com.example.safariwave_backend.service.payment;

import com.example.safariwave_backend.dto.request.PaymentRequestDTO;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import org.springframework.stereotype.Component;

/**
 * Cash paid at the office: a staff member confirms they received the full amount.
 * Customers cannot use it, so nobody can mark their own booking as paid.
 */
@Component
public class CashPaymentStrategy implements PaymentStrategy {

    public static final String METHOD = "CASH";

    @Override
    public boolean supports(String method) {
        return METHOD.equals(method);
    }

    @Override
    public String provider() {
        return "CASH";
    }

    @Override
    public PaymentOutcome pay(Booking booking, PaymentRequestDTO request) {
        CurrentUser.requireAnyRole(Roles.CUSTOMER_RELATIONS, Roles.FINANCE);
        return PaymentOutcome.success();
    }
}
