package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A guide (or operations) reports a problem with a boat; it becomes an OPEN maintenance record. */
public class IssueReportRequestDTO {

    @NotNull
    private Integer boatId;

    @NotBlank
    @Size(max = 2000)
    private String description;

    public IssueReportRequestDTO() {
    }

    public Integer getBoatId() {
        return boatId;
    }

    public void setBoatId(Integer boatId) {
        this.boatId = boatId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
