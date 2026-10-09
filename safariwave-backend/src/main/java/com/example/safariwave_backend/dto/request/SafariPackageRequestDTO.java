package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class SafariPackageRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String packageName;
    @Size(max = 5000)
    private String description;
    @NotNull
    @Min(1)
    private Integer duration;
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal basePrice;
    @NotNull
    @Min(1)
    private Integer maxPassengers;
    @NotBlank
    @Pattern(regexp = "(?i)ACTIVE|INACTIVE", message = "must be ACTIVE or INACTIVE")
    private String status;

    public SafariPackageRequestDTO() {
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getMaxPassengers() {
        return maxPassengers;
    }

    public void setMaxPassengers(Integer maxPassengers) {
        this.maxPassengers = maxPassengers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}