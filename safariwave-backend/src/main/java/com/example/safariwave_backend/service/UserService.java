package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.UserRegisterDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {

    UserResponseDTO registerUser(UserRegisterDTO dto);

    Optional<UserResponseDTO> getUserById(Integer userId);

    Optional<UserResponseDTO> getUserByEmail(String email);

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO updateUser(Integer userId, UserRegisterDTO dto);

    void deleteUser(Integer userId);
}