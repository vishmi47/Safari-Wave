package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.UserRegisterDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<UserResponseDTO> registerUser(@Valid @RequestBody UserRegisterDTO dto) {
        // This endpoint is public for sign-up; only an ADMIN may choose the role of the new account.
        UserResponseDTO savedUser = CurrentUser.isAdmin()
                ? userService.registerUser(dto)
                : userService.registerCustomer(dto);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Customer accounts only (ADMIN and CUSTOMER_RELATIONS), used when booking on a customer's behalf
    @GetMapping("/customers")
    public ResponseEntity<List<UserResponseDTO>> getCustomers() {
        return ResponseEntity.ok(userService.getCustomers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Integer id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Integer id, @Valid @RequestBody UserRegisterDTO dto) {
        UserResponseDTO updated = userService.updateUser(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}