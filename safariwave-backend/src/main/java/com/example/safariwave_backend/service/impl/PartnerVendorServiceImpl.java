package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.PartnerVendorRequestDTO;
import com.example.safariwave_backend.dto.response.PartnerVendorResponseDTO;
import com.example.safariwave_backend.entity.PartnerVendorEntity;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.DuplicateResourceException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.PartnerVendorRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.PartnerVendorNotificationService;
import com.example.safariwave_backend.service.PartnerVendorService;
import com.example.safariwave_backend.service.support.PagingSupport;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Partner & Vendor management (spec 6.6 / B.3).
 *
 * ADMIN: create, read, update, approve/reject/terminate, delete (soft by default; permanent only once
 * INACTIVE or TERMINATED). FINANCE: read all. OPERATIONS: read equipment suppliers and transport.
 * MARKETING: read hotels and travel agencies. CUSTOMER and other roles: no access.
 */
@Service
@Transactional
public class PartnerVendorServiceImpl implements PartnerVendorService {

    static final List<String> PARTNER_TYPES = List.of("HOTEL", "TRAVEL_AGENCY", "TRANSPORT", "RESTAURANT", "EQUIPMENT_SUPPLIER");
    private static final Map<String, List<String>> VISIBLE_TYPES_BY_ROLE = Map.of(
            Roles.OPERATIONS, List.of("EQUIPMENT_SUPPLIER", "TRANSPORT"),
            Roles.MARKETING, List.of("HOTEL", "TRAVEL_AGENCY"));
    private static final Path DOCUMENT_DIRECTORY = Paths.get("uploads-private", "partners-vendors").toAbsolutePath().normalize();
    private static final String DOCUMENT_URL_PREFIX = "/api/partners-vendors/documents/";

    private final PartnerVendorRepository partnerVendorRepository;
    private final PartnerVendorNotificationService notificationService;

    public PartnerVendorServiceImpl(PartnerVendorRepository partnerVendorRepository,
                                    PartnerVendorNotificationService notificationService) {
        this.partnerVendorRepository = partnerVendorRepository;
        this.notificationService = notificationService;
    }

    @Override
    public PartnerVendorResponseDTO createPartnerVendor(PartnerVendorRequestDTO dto) {
        CurrentUser.requireAnyRole(); // ADMIN only
        validateContractDates(dto);
        if (partnerVendorRepository.existsByBusinessNameIgnoreCase(dto.getBusinessName())) {
            throw new DuplicateResourceException("Partner/vendor business name already exists.");
        }
        if (partnerVendorRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new DuplicateResourceException("Partner/vendor email already exists.");
        }

        PartnerVendorEntity entity = new PartnerVendorEntity();
        copyRequestToEntity(dto, entity);
        entity.setStatus(PartnerVendorStatus.PENDING_APPROVAL);
        entity.setCreatedBy(CurrentUser.find().map(user -> user.userId()).orElse(null));

        PartnerVendorEntity saved = partnerVendorRepository.save(entity);
        notificationService.notifyRegistration(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerVendorResponseDTO> getAllPartnerVendors() {
        List<String> types = visibleTypes();
        return partnerVendorRepository.findAll().stream().filter(entity -> canSee(entity, types)).map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerVendorResponseDTO getPartnerVendorById(Long id) {
        List<String> types = visibleTypes();
        PartnerVendorEntity entity = findById(id);
        if (!canSee(entity, types)) {
            throw new AccessDeniedException("You are not authorized to view this partner.");
        }
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerVendorResponseDTO> getPartnerVendorsByStatus(PartnerVendorStatus status) {
        List<String> types = visibleTypes();
        return partnerVendorRepository.findByStatus(status).stream().filter(entity -> canSee(entity, types)).map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PartnerVendorResponseDTO> searchPartnerVendors(String query, String status, String type, int page, int size) {
        List<String> visible = visibleTypes();
        List<String> types = visible == null ? PARTNER_TYPES : visible;
        boolean allTypes = visible == null;
        if (type != null && !type.isBlank()) {
            String requested = normalizeType(type);
            if (!types.contains(requested)) {
                throw new BadRequestException("Type must be one of " + types + ".");
            }
            types = List.of(requested);
            allTypes = false;
        }
        PartnerVendorStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = PartnerVendorStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Status must be one of " + Arrays.toString(PartnerVendorStatus.values()) + ".");
            }
        }
        return partnerVendorRepository.search(PagingSupport.text(query), statusFilter, allTypes, types,
                        PagingSupport.pageRequest(page, size, Sort.by("businessName")))
                .map(this::toResponse);
    }

    @Override
    public PartnerVendorResponseDTO updatePartnerVendor(Long id, PartnerVendorRequestDTO dto) {
        CurrentUser.requireAnyRole(); // ADMIN only
        validateContractDates(dto);
        PartnerVendorEntity entity = findById(id);

        partnerVendorRepository.findByEmailIgnoreCase(dto.getEmail()).ifPresent(existing -> {
            if (!existing.getPartnerVendorId().equals(id)) {
                throw new DuplicateResourceException("Partner/vendor email already exists.");
            }
        });

        if (!entity.getBusinessName().equalsIgnoreCase(dto.getBusinessName())
                && partnerVendorRepository.existsByBusinessNameIgnoreCase(dto.getBusinessName())) {
            throw new DuplicateResourceException("Partner/vendor business name already exists.");
        }

        copyRequestToEntity(dto, entity);
        return toResponse(partnerVendorRepository.save(entity));
    }

    @Override
    public void deletePartnerVendor(Long id, boolean permanent) {
        CurrentUser.requireAnyRole(); // ADMIN only
        PartnerVendorEntity entity = findById(id);
        if (!permanent) {
            // Spec B.3: prefer an INACTIVE/TERMINATED status over hard delete
            entity.setStatus(PartnerVendorStatus.INACTIVE);
            partnerVendorRepository.save(entity);
            return;
        }
        if (entity.getStatus() != PartnerVendorStatus.INACTIVE && entity.getStatus() != PartnerVendorStatus.TERMINATED) {
            throw new BadRequestException("Deactivate or terminate the partner before deleting it permanently.");
        }
        partnerVendorRepository.delete(entity);
    }

    @Override
    public PartnerVendorResponseDTO approvePartnerVendor(Long id) {
        return changeStatus(id, PartnerVendorStatus.APPROVED);
    }

    @Override
    public PartnerVendorResponseDTO rejectPartnerVendor(Long id) {
        return changeStatus(id, PartnerVendorStatus.REJECTED);
    }

    @Override
    public PartnerVendorResponseDTO terminatePartnerVendor(Long id) {
        return changeStatus(id, PartnerVendorStatus.TERMINATED);
    }

    @Override
    public String storeDocument(MultipartFile file) {
        CurrentUser.requireAnyRole(); // ADMIN only
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Document file is required.");
        }
        String contentType = file.getContentType();
        String extension = "application/pdf".equals(contentType) ? ".pdf"
                : "image/jpeg".equals(contentType) ? ".jpg"
                : "image/png".equals(contentType) ? ".png" : null;
        if (extension == null) {
            throw new BadRequestException("Only PDF, JPG, and PNG files are allowed.");
        }
        try {
            Files.createDirectories(DOCUMENT_DIRECTORY);
            String filename = UUID.randomUUID() + extension;
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, DOCUMENT_DIRECTORY.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
            return DOCUMENT_URL_PREFIX + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store the document.", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadDocument(String filename) {
        if (filename == null || !filename.matches("[0-9a-f\\-]{36}\\.(pdf|jpg|png)")) {
            throw new ResourceNotFoundException("Document not found.");
        }
        List<String> types = visibleTypes();
        PartnerVendorEntity owner = partnerVendorRepository.findFirstByDocumentUrl(DOCUMENT_URL_PREFIX + filename).orElse(null);
        // A document not yet attached to a partner is only visible to the admin who uploaded it
        if (owner == null ? types != null : !canSee(owner, types)) {
            throw new AccessDeniedException("You are not authorized to view this document.");
        }
        Path path = DOCUMENT_DIRECTORY.resolve(filename).normalize();
        if (!path.startsWith(DOCUMENT_DIRECTORY) || !Files.exists(path)) {
            throw new ResourceNotFoundException("Document not found.");
        }
        return new FileSystemResource(path);
    }

    private PartnerVendorResponseDTO changeStatus(Long id, PartnerVendorStatus status) {
        CurrentUser.requireAnyRole(); // ADMIN only
        PartnerVendorEntity entity = findById(id);
        entity.setStatus(status);
        PartnerVendorEntity saved = partnerVendorRepository.save(entity);
        notificationService.notifyStatusChange(saved, status);
        return toResponse(saved);
    }

    /** Partner types the logged-in user may read; null means every type. */
    private List<String> visibleTypes() {
        if (CurrentUser.isAdmin() || CurrentUser.hasAnyRole(Roles.FINANCE)) {
            return null;
        }
        List<String> types = VISIBLE_TYPES_BY_ROLE.get(CurrentUser.role());
        if (types == null) {
            throw new AccessDeniedException("You are not authorized to view partners and vendors.");
        }
        return types;
    }

    private boolean canSee(PartnerVendorEntity entity, List<String> types) {
        return types == null || types.contains(normalizeType(entity.getPartnerType()));
    }

    private String normalizeType(String type) {
        return type == null ? null : type.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private PartnerVendorEntity findById(Long id) {
        return partnerVendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner/vendor not found with ID: " + id));
    }

    private void validateContractDates(PartnerVendorRequestDTO dto) {
        if (dto.getContractStartDate() != null && dto.getContractEndDate() != null
                && dto.getContractStartDate().isAfter(dto.getContractEndDate())) {
            throw new IllegalArgumentException("Contract start date cannot be after contract end date.");
        }
    }

    private void copyRequestToEntity(PartnerVendorRequestDTO dto, PartnerVendorEntity entity) {
        String type = normalizeType(dto.getPartnerType());
        if (!PARTNER_TYPES.contains(type)) {
            throw new BadRequestException("Partner type must be one of " + PARTNER_TYPES + ".");
        }
        entity.setBusinessName(dto.getBusinessName());
        entity.setPartnerType(type);
        entity.setDescription(dto.getDescription());
        entity.setContactPerson(dto.getContactPerson());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setAddress(dto.getAddress());
        entity.setServiceType(dto.getServiceType());
        entity.setServiceDescription(dto.getServiceDescription());
        entity.setContractNumber(dto.getContractNumber());
        entity.setContractStartDate(dto.getContractStartDate());
        entity.setContractEndDate(dto.getContractEndDate());
        entity.setAgreementDetails(dto.getAgreementDetails());
        entity.setDocumentUrl(dto.getDocumentUrl());
    }

    private PartnerVendorResponseDTO toResponse(PartnerVendorEntity entity) {
        PartnerVendorResponseDTO response = new PartnerVendorResponseDTO();
        response.setPartnerVendorId(entity.getPartnerVendorId());
        response.setBusinessName(entity.getBusinessName());
        response.setPartnerType(entity.getPartnerType());
        response.setDescription(entity.getDescription());
        response.setContactPerson(entity.getContactPerson());
        response.setEmail(entity.getEmail());
        response.setPhone(entity.getPhone());
        response.setAddress(entity.getAddress());
        response.setServiceType(entity.getServiceType());
        response.setServiceDescription(entity.getServiceDescription());
        response.setContractNumber(entity.getContractNumber());
        response.setContractStartDate(entity.getContractStartDate());
        response.setContractEndDate(entity.getContractEndDate());
        response.setAgreementDetails(entity.getAgreementDetails());
        response.setDocumentUrl(entity.getDocumentUrl());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        return response;
    }
}
