package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.SafariPackageRequestDTO;
import com.example.safariwave_backend.dto.response.SafariPackageResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface SafariPackageService {

    SafariPackageResponseDTO createPackage(SafariPackageRequestDTO dto);

    Optional<SafariPackageResponseDTO> getPackageById(Integer packageId);

    List<SafariPackageResponseDTO> getAllPackages();

    SafariPackageResponseDTO updatePackage(Integer packageId, SafariPackageRequestDTO dto);

    /** Hard-deletes an unused package; archives it (status ARCHIVED) when trips, bookings or promotions use it. */
    void deletePackage(Integer packageId);

    /** Paged search by name/description and status. Customers only ever see ACTIVE packages. */
    Page<SafariPackageResponseDTO> searchPackages(String query, String status, int page, int size);
}