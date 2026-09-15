package com.example.safariwave_backend.dto.request;

public class ComplaintStatusUpdateRequestDTO {

    private String status;
    private String internalNotes;

    public ComplaintStatusUpdateRequestDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }
}
