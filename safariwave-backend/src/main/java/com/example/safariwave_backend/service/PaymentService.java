package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.PaymentRequestDTO;
import com.example.safariwave_backend.dto.response.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO processMockPayment(Integer bookingId, PaymentRequestDTO request);
    PaymentResponseDTO getByBooking(Integer bookingId);
}
