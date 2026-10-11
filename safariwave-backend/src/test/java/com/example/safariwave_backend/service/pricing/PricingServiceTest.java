package com.example.safariwave_backend.service.pricing;

import com.example.safariwave_backend.entity.Promotion;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    private PromotionRepository promotionRepository;
    private SafariPackage safariPackage;

    @BeforeEach
    void setUp() {
        promotionRepository = mock(PromotionRepository.class);
        safariPackage = new SafariPackage();
        safariPackage.setPackageId(1);
        safariPackage.setBasePrice(new BigDecimal("5000.00"));
    }

    private static Promotion promotion(String percent) {
        Promotion promotion = new Promotion();
        promotion.setDiscountPercentage(percent == null ? null : new BigDecimal(percent));
        return promotion;
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    // ---- individual strategies ----

    @Test
    void noDiscountStrategy_alwaysGivesZero() {
        assertAmount("0", new NoDiscountStrategy().discountPercentage(safariPackage, TODAY));
    }

    @Test
    void promotionStrategy_picksTheHighestPublishedPromotion_andIgnoresMissingValues() {
        when(promotionRepository.findPublishedForPackageOn(eq(1), eq(TODAY)))
                .thenReturn(List.of(promotion("10"), promotion(null), promotion("25"), promotion("15")));

        assertAmount("25", new PromotionDiscountStrategy(promotionRepository).discountPercentage(safariPackage, TODAY));
    }

    @Test
    void promotionStrategy_givesZeroWhenNoPromotionIsRunning() {
        when(promotionRepository.findPublishedForPackageOn(eq(1), any(LocalDate.class))).thenReturn(List.of());

        assertAmount("0", new PromotionDiscountStrategy(promotionRepository).discountPercentage(safariPackage, TODAY));
    }

    // ---- context: chooses between strategies ----

    @Test
    void bestDiscount_usesTheHighestDiscountOfAllStrategies() {
        when(promotionRepository.findPublishedForPackageOn(eq(1), eq(TODAY))).thenReturn(List.of(promotion("20")));
        PricingStrategy seasonal = (pkg, date) -> new BigDecimal("30");
        PricingService pricing = new PricingService(List.of(new PromotionDiscountStrategy(promotionRepository), new NoDiscountStrategy(), seasonal));

        assertAmount("30", pricing.bestDiscount(safariPackage, TODAY));
    }

    @Test
    void bestDiscount_fallsBackToNoDiscount() {
        when(promotionRepository.findPublishedForPackageOn(eq(1), eq(TODAY))).thenReturn(List.of());
        PricingService pricing = new PricingService(List.of(new PromotionDiscountStrategy(promotionRepository), new NoDiscountStrategy()));

        assertAmount("0", pricing.bestDiscount(safariPackage, TODAY));
    }

    // ---- totals (same results as before the pattern was introduced) ----

    @Test
    void total_appliesTheDiscountToAllPassengers() {
        PricingService pricing = new PricingService(List.of(new NoDiscountStrategy()));

        assertAmount("10000.00", pricing.total(new BigDecimal("5000.00"), 2, BigDecimal.ZERO));
        assertAmount("9000.00", pricing.total(new BigDecimal("5000.00"), 2, new BigDecimal("10")));
        assertAmount("2500.00", pricing.total(new BigDecimal("2500.00"), 1, null));
        assertAmount("2333.33", pricing.total(new BigDecimal("1000.00"), 3, new BigDecimal("22.2222")));
    }
}
