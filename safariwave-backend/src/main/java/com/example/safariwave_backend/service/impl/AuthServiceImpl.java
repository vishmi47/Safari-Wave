package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.LoginRequestDTO;
import com.example.safariwave_backend.dto.response.LoginResponseDTO;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.JwtUtil;
import com.example.safariwave_backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getUserId(), user.getRole().getRoleName());

        return new LoginResponseDTO(token, user.getUserId(), user.getEmail(), user.getRole().getRoleName());
    }
}