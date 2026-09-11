package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.SafariPackageRequestDTO;
import com.example.safariwave_backend.dto.response.SafariPackageResponseDTO;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.service.SafariPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SafariPackageServiceImpl implements SafariPackageService {

    @Autowired
    private SafariPackageRepository safariPackageRepository;

    @Override
    public SafariPackageResponseDTO createPackage(SafariPackageRequestDTO dto) {
        SafariPackage pkg = new SafariPackage();
        pkg.setPackageName(dto.getPackageName());
        pkg.setDescription(dto.getDescription());
        pkg.setDuration(dto.getDuration());
        pkg.setBasePrice(dto.getBasePrice());
        pkg.setMaxPassengers(dto.getMaxPassengers());
        pkg.setStatus(dto.getStatus());

        SafariPackage saved = safariPackageRepository.save(pkg);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<SafariPackageResponseDTO> getPackageById(Integer packageId) {
        return safariPackageRepository.findById(packageId).map(this::toResponseDTO);
    }

    @Override
    public List<SafariPackageResponseDTO> getAllPackages() {
        return safariPackageRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SafariPackageResponseDTO updatePackage(Integer packageId, SafariPackageRequestDTO dto) {
        SafariPackage existing = safariPackageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found with id: " + packageId));

        existing.setPackageName(dto.getPackageName());
        existing.setDescription(dto.getDescription());
        existing.setDuration(dto.getDuration());
        existing.setBasePrice(dto.getBasePrice());
        existing.setMaxPassengers(dto.getMaxPassengers());
        existing.setStatus(dto.getStatus());

        SafariPackage updated = safariPackageRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deletePackage(Integer packageId) {
        if (!safariPackageRepository.existsById(packageId)) {
            throw new RuntimeException("Package not found with id: " + packageId);
        }
        safariPackageRepository.deleteById(packageId);
    }

    private SafariPackageResponseDTO toResponseDTO(SafariPackage pkg) {
        SafariPackageResponseDTO dto = new SafariPackageResponseDTO();
        dto.setPackageId(pkg.getPackageId());
        dto.setPackageName(pkg.getPackageName());
        dto.setDescription(pkg.getDescription());
        dto.setDuration(pkg.getDuration());
        dto.setBasePrice(pkg.getBasePrice());
        dto.setMaxPassengers(pkg.getMaxPassengers());
        dto.setStatus(pkg.getStatus());
        dto.setCreatedAt(pkg.getCreatedAt());
        dto.setUpdatedAt(pkg.getUpdatedAt());
        return dto;
    }
}