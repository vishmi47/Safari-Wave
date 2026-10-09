package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Create / update body for a promotion (sent as the "promotion" JSON part of a multipart request, or as plain JSON). */
public class PromotionRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String promotionName;

    @Size(max = 5000)
    private String description;

    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "must be greater than 0")
    @DecimalMax(value = "100", message = "must be at most 100")
    private BigDecimal discountPercentage;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull(message = "a package must be selected")
    private Integer packageId;

    // DRAFT (default) or PUBLISHED
    private String status;

    public PromotionRequestDTO() {
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    /** Accepts the older request shape {"safariPackage": {"packageId": 1}} used before this DTO existed. */
    public void setSafariPackage(Map<String, Object> safariPackage) {
        if (safariPackage != null && safariPackage.get("packageId") instanceof Number number) {
            this.packageId = number.intValue();
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
