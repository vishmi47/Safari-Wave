package com.example.safariwave_backend.service.payment;

import com.example.safariwave_backend.dto.request.PaymentRequestDTO;
import com.example.safariwave_backend.entity.Booking;

/**
 * Strategy pattern (behavioural): one way of taking payment for a booking (card, cash, ...).
 *
 * PaymentContext picks the strategy whose supports() matches the requested method.
 * A new payment method is added by writing a new class; PaymentServiceImpl does not change.
 */
public interface PaymentStrategy {

    /** True when this strategy handles the requested payment method (e.g. "CARD", "CASH"). */
    boolean supports(String method);

    /** Stored on the Payment record so finance can see how it was paid. */
    String provider();

    /** Takes the payment and reports whether it succeeded. Throws when the request is not allowed. */
    PaymentOutcome pay(Booking booking, PaymentRequestDTO request);
}
