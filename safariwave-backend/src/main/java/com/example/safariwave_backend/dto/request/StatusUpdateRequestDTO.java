package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for status-only updates, e.g. trip status or booking payment status. */
public class StatusUpdateRequestDTO {

    @NotBlank
    private String status;

    @Size(max = 255)
    private String remarks;

    public StatusUpdateRequestDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
