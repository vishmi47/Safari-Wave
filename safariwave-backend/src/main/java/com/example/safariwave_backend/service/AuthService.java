package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.LoginRequestDTO;
import com.example.safariwave_backend.dto.response.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO dto);
}