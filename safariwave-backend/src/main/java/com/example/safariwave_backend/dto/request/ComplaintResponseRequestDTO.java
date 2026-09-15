package com.example.safariwave_backend.dto.request;

public class ComplaintResponseRequestDTO {

    private String response;
    private String status;

    public ComplaintResponseRequestDTO() {
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
