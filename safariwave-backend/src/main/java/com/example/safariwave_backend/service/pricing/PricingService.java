package com.example.safariwave_backend.service.pricing;

import com.example.safariwave_backend.entity.SafariPackage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Context of the Strategy pattern: asks every PricingStrategy for its discount and applies the best one.
 * Discounts do not stack; the customer gets the single highest discount.
 */
@Service
public class PricingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final List<PricingStrategy> strategies;

    public PricingService(List<PricingStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    /** Highest discount (percent) any strategy gives for the package on the date. */
    public BigDecimal bestDiscount(SafariPackage safariPackage, LocalDate bookingDate) {
        return strategies.stream()
                .map(strategy -> strategy.discountPercentage(safariPackage, bookingDate))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    /** unit price x passengers, minus the discount, rounded to 2 decimals. */
    public BigDecimal total(BigDecimal unitPrice, int passengers, BigDecimal discountPercentage) {
        BigDecimal discount = discountPercentage == null ? BigDecimal.ZERO : discountPercentage;
        BigDecimal multiplier = HUNDRED.subtract(discount).divide(HUNDRED);
        return unitPrice
                .multiply(BigDecimal.valueOf(passengers))
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
