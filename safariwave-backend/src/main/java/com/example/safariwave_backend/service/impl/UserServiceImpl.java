package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.UserRegisterDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;
import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.repository.RoleRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO registerUser(UserRegisterDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered: " + dto.getEmail());
        }

        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + dto.getRoleId()));

        User user = new User();
        user.setRole(role);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword())); // ✅ now hashed
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus());

        User saved = userRepository.save(user);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<UserResponseDTO> getUserById(Integer userId) {
        return userRepository.findById(userId).map(this::toResponseDTO);
    }

    @Override
    public Optional<UserResponseDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toResponseDTO);
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO updateUser(Integer userId, UserRegisterDTO dto) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        existingUser.setFirstName(dto.getFirstName());
        existingUser.setLastName(dto.getLastName());
        existingUser.setPhone(dto.getPhone());
        existingUser.setStatus(dto.getStatus());

        User updated = userRepository.save(existingUser);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteUser(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    private UserResponseDTO toResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(user.getUserId());
        dto.setRoleId(user.getRole().getRoleId());
        dto.setRoleName(user.getRole().getRoleName());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}