package com.example.safariwave_backend.service.pricing;

import com.example.safariwave_backend.entity.SafariPackage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Default strategy: the full base price. Guarantees a result when no other strategy applies. */
@Component
public class NoDiscountStrategy implements PricingStrategy {

    @Override
    public BigDecimal discountPercentage(SafariPackage safariPackage, LocalDate bookingDate) {
        return BigDecimal.ZERO;
    }
}
