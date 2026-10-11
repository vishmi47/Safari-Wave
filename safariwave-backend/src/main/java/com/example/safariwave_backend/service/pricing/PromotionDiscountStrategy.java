package com.example.safariwave_backend.service.pricing;

import com.example.safariwave_backend.entity.Promotion;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.repository.PromotionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** The best published promotion for the package that is valid on the booking date. */
@Component
public class PromotionDiscountStrategy implements PricingStrategy {

    private final PromotionRepository promotionRepository;

    public PromotionDiscountStrategy(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public BigDecimal discountPercentage(SafariPackage safariPackage, LocalDate bookingDate) {
        return promotionRepository.findPublishedForPackageOn(safariPackage.getPackageId(), bookingDate)
                .stream()
                .map(Promotion::getDiscountPercentage)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}
