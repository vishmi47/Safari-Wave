package com.example.safariwave_backend.service.payment;

import com.example.safariwave_backend.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Context of the Strategy pattern: chooses the PaymentStrategy for the requested method at runtime.
 * Spring passes in every PaymentStrategy bean, so a new strategy class is picked up without changes here.
 */
@Component
public class PaymentContext {

    private final List<PaymentStrategy> strategies;

    public PaymentContext(List<PaymentStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    /** No method given = card, which is what the customer's "Pay" button has always done. */
    public PaymentStrategy strategyFor(String method) {
        String wanted = method == null || method.isBlank() ? CardPaymentStrategy.METHOD : method.trim().toUpperCase(Locale.ROOT);
        return strategies.stream()
                .filter(strategy -> strategy.supports(wanted))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported payment method: " + method));
    }
}
