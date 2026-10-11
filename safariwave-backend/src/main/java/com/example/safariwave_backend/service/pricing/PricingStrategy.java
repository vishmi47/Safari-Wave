package com.example.safariwave_backend.service.pricing;

import com.example.safariwave_backend.entity.SafariPackage;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Strategy pattern (behavioural): one way of working out a discount for a booking.
 *
 * Each implementation is a Spring bean, so PricingService receives all of them automatically.
 * A new kind of discount is added by writing a new class; BookingServiceImpl does not change.
 */
public interface PricingStrategy {

    /** Discount in percent (0-100) this strategy gives for the package on the booking date. */
    BigDecimal discountPercentage(SafariPackage safariPackage, LocalDate bookingDate);
}
