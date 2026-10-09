package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.UserRegisterDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {

    /** Staff/admin account creation: uses the requested roleId. Only called for ADMIN callers. */
    UserResponseDTO registerUser(UserRegisterDTO dto);

    /** Public self-registration: always creates an ACTIVE CUSTOMER, whatever role was requested. */
    UserResponseDTO registerCustomer(UserRegisterDTO dto);

    Optional<UserResponseDTO> getUserById(Integer userId);

    Optional<UserResponseDTO> getUserByEmail(String email);

    List<UserResponseDTO> getAllUsers();

    /** Users with the CUSTOMER role, sorted by first name. */
    List<UserResponseDTO> getCustomers();

    UserResponseDTO updateUser(Integer userId, UserRegisterDTO dto);

    void deleteUser(Integer userId);
}