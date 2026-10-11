package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.UserRegisterDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;
import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.DuplicateResourceException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.RoleRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.UserService;
import com.example.safariwave_backend.service.factory.UserFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    /** Factory pattern: builds every new User (see service/factory). */
    @Autowired
    private UserFactory userFactory;

    @Override
    public UserResponseDTO registerUser(UserRegisterDTO dto) {
        if (dto.getRoleId() == null) {
            throw new BadRequestException("Role is required.");
        }
        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + dto.getRoleId()));
        requireUniqueEmail(dto.getEmail());
        return toResponseDTO(userRepository.save(userFactory.newAccountWithRole(dto, role)));
    }

    @Override
    public UserResponseDTO registerCustomer(UserRegisterDTO dto) {
        // Self-registration: the requested roleId/status are ignored so nobody can sign up as staff.
        Role role = roleRepository.findFirstByRoleNameIgnoreCase(Roles.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Customer role is not configured."));
        requireUniqueEmail(dto.getEmail());
        return toResponseDTO(userRepository.save(userFactory.newCustomer(dto, role)));
    }

    private void requireUniqueEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
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
    public List<UserResponseDTO> getCustomers() {
        return userRepository.findByRole_RoleNameInOrderByFirstNameAsc(List.of(Roles.CUSTOMER))
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO updateUser(Integer userId, UserRegisterDTO dto) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

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
            throw new ResourceNotFoundException("User not found with id: " + userId);
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