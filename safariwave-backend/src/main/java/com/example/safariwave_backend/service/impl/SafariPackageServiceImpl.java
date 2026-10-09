package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.SafariPackageRequestDTO;
import com.example.safariwave_backend.dto.response.SafariPackageResponseDTO;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.PromotionRepository;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.SafariPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Safari Package CRUD (spec 6.3 / B.3 "Promotion & Safari Package").
 *
 * Permissions: MARKETING and ADMIN create, update and delete; CUSTOMER_RELATIONS, FINANCE and OPERATIONS
 * read everything; CUSTOMER reads ACTIVE packages only; GUIDE has no access.
 */
@Service
public class SafariPackageServiceImpl implements SafariPackageService {

    static final String ACTIVE = "ACTIVE";
    static final String INACTIVE = "INACTIVE";
    static final String ARCHIVED = "ARCHIVED";

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private SafariPackageRepository safariPackageRepository;

    @Autowired
    private SafariTripRepository safariTripRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @Override
    public SafariPackageResponseDTO createPackage(SafariPackageRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.MARKETING);

        SafariPackage pkg = new SafariPackage();
        copyRequestToEntity(dto, pkg);
        pkg.setCreatedBy(CurrentUser.id());

        SafariPackage saved = safariPackageRepository.save(pkg);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<SafariPackageResponseDTO> getPackageById(Integer packageId) {
        requireReadAccess();
        return safariPackageRepository.findById(packageId)
                .filter(pkg -> !isCustomer() || ACTIVE.equals(pkg.getStatus()))
                .map(this::toResponseDTO);
    }

    @Override
    public List<SafariPackageResponseDTO> getAllPackages() {
        requireReadAccess();
        List<SafariPackage> packages = isCustomer()
                ? safariPackageRepository.findByStatus(ACTIVE)
                : safariPackageRepository.findByStatusIsNullOrStatusNot(ARCHIVED);
        return packages
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SafariPackageResponseDTO updatePackage(Integer packageId, SafariPackageRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.MARKETING);

        SafariPackage existing = findPackage(packageId);
        copyRequestToEntity(dto, existing);

        SafariPackage updated = safariPackageRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deletePackage(Integer packageId) {
        CurrentUser.requireAnyRole(Roles.MARKETING);

        SafariPackage pkg = findPackage(packageId);
        // Spec B.3: MARKETING may only delete inactive items; ADMIN may delete any package.
        if (!CurrentUser.isAdmin() && ACTIVE.equals(pkg.getStatus())) {
            throw new BadRequestException("Only inactive packages can be deleted. Set the package to Inactive first.");
        }

        if (isReferenced(packageId)) {
            // Soft delete: trips, bookings or promotions still point at this package.
            pkg.setStatus(ARCHIVED);
            safariPackageRepository.save(pkg);
        } else {
            safariPackageRepository.delete(pkg);
        }
    }

    @Override
    public Page<SafariPackageResponseDTO> searchPackages(String query, String status, int page, int size) {
        requireReadAccess();
        if (page < 0) {
            throw new BadRequestException("Page must be 0 or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }

        String statusFilter = isCustomer() ? ACTIVE : normalizeOptional(status);
        if (statusFilter != null && !List.of(ACTIVE, INACTIVE, ARCHIVED).contains(statusFilter)) {
            throw new BadRequestException("Status must be ACTIVE, INACTIVE or ARCHIVED.");
        }
        String text = query == null || query.isBlank() ? null : query.trim();

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("packageName").ascending());
        return safariPackageRepository.search(text, statusFilter, pageRequest).map(this::toResponseDTO);
    }

    private void requireReadAccess() {
        CurrentUser.requireAnyRole(Roles.MARKETING, Roles.CUSTOMER_RELATIONS, Roles.FINANCE, Roles.OPERATIONS, Roles.CUSTOMER);
    }

    private boolean isCustomer() {
        return CurrentUser.hasAnyRole(Roles.CUSTOMER);
    }

    private boolean isReferenced(Integer packageId) {
        return safariTripRepository.existsBySafariPackage_PackageId(packageId)
                || bookingRepository.existsBySafariPackage_PackageId(packageId)
                || promotionRepository.existsBySafariPackage_PackageId(packageId);
    }

    private SafariPackage findPackage(Integer packageId) {
        return safariPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));
    }

    private void copyRequestToEntity(SafariPackageRequestDTO dto, SafariPackage pkg) {
        String status = normalizeOptional(dto.getStatus());
        if (!ACTIVE.equals(status) && !INACTIVE.equals(status)) {
            throw new BadRequestException("Status must be ACTIVE or INACTIVE.");
        }
        pkg.setPackageName(dto.getPackageName().trim());
        pkg.setDescription(dto.getDescription());
        pkg.setDuration(dto.getDuration());
        pkg.setBasePrice(dto.getBasePrice());
        pkg.setMaxPassengers(dto.getMaxPassengers());
        pkg.setStatus(status);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
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
        dto.setCreatedBy(pkg.getCreatedBy());
        return dto;
    }
}
