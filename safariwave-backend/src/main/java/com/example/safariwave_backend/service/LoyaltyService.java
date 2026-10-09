package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.response.LoyaltyResponseDTO;

public interface LoyaltyService {
    LoyaltyResponseDTO enroll(Integer customerId);
    LoyaltyResponseDTO getByCustomer(Integer customerId);
    LoyaltyResponseDTO updateStatus(Integer customerId, String status);
    LoyaltyResponseDTO addPoints(Integer customerId, long points);
}
