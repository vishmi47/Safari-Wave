package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.SafariPackageRequestDTO;
import com.example.safariwave_backend.dto.response.SafariPackageResponseDTO;

import java.util.List;
import java.util.Optional;

public interface SafariPackageService {

    SafariPackageResponseDTO createPackage(SafariPackageRequestDTO dto);

    Optional<SafariPackageResponseDTO> getPackageById(Integer packageId);

    List<SafariPackageResponseDTO> getAllPackages();

    SafariPackageResponseDTO updatePackage(Integer packageId, SafariPackageRequestDTO dto);

    void deletePackage(Integer packageId);
}