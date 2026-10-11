package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class WeatherAlertRequestDTO {

    @NotNull
    private LocalDateTime alertDateTime;
    @NotBlank
    @Size(max = 150)
    private String location;
    @NotBlank
    @Size(max = 100)
    private String weatherCondition;
    @NotBlank
    private String severity;
    @Size(max = 2000)
    private String description;
    private String alertStatus;
    @Size(max = 1000)
    private String remarks;
    private List<Integer> affectedIncidentIds;

    public WeatherAlertRequestDTO() {
    }

    public LocalDateTime getAlertDateTime() {
        return alertDateTime;
    }

    public void setAlertDateTime(LocalDateTime alertDateTime) {
        this.alertDateTime = alertDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAlertStatus() {
        return alertStatus;
    }

    public void setAlertStatus(String alertStatus) {
        this.alertStatus = alertStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public List<Integer> getAffectedIncidentIds() {
        return affectedIncidentIds;
    }

    public void setAffectedIncidentIds(List<Integer> affectedIncidentIds) {
        this.affectedIncidentIds = affectedIncidentIds;
    }
}
