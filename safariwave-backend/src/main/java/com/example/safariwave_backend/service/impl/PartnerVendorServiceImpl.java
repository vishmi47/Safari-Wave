package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.PartnerVendorRequestDTO;
import com.example.safariwave_backend.dto.response.PartnerVendorResponseDTO;
import com.example.safariwave_backend.entity.PartnerVendorEntity;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import com.example.safariwave_backend.exception.DuplicateResourceException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.PartnerVendorRepository;
import com.example.safariwave_backend.service.PartnerVendorNotificationService;
import com.example.safariwave_backend.service.PartnerVendorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PartnerVendorServiceImpl implements PartnerVendorService {

    private final PartnerVendorRepository partnerVendorRepository;
    private final PartnerVendorNotificationService notificationService;

    public PartnerVendorServiceImpl(PartnerVendorRepository partnerVendorRepository,
                                    PartnerVendorNotificationService notificationService) {
        this.partnerVendorRepository = partnerVendorRepository;
        this.notificationService = notificationService;
    }

    @Override
    public PartnerVendorResponseDTO createPartnerVendor(PartnerVendorRequestDTO dto) {
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
        PartnerVendorEntity saved = partnerVendorRepository.save(entity);
        notificationService.notifyRegistration(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerVendorResponseDTO> getAllPartnerVendors() {
        return partnerVendorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerVendorResponseDTO getPartnerVendorById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerVendorResponseDTO> getPartnerVendorsByStatus(PartnerVendorStatus status) {
        return partnerVendorRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Override
    public PartnerVendorResponseDTO updatePartnerVendor(Long id, PartnerVendorRequestDTO dto) {
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
    public void deletePartnerVendor(Long id) {
        PartnerVendorEntity entity = findById(id);
        entity.setStatus(PartnerVendorStatus.INACTIVE);
        partnerVendorRepository.save(entity);
    }

    @Override
    public PartnerVendorResponseDTO approvePartnerVendor(Long id) {
        return changeStatus(id, PartnerVendorStatus.APPROVED);
    }

    @Override
    public PartnerVendorResponseDTO rejectPartnerVendor(Long id) {
        return changeStatus(id, PartnerVendorStatus.REJECTED);
    }

    private PartnerVendorResponseDTO changeStatus(Long id, PartnerVendorStatus status) {
        PartnerVendorEntity entity = findById(id);
        entity.setStatus(status);
        PartnerVendorEntity saved = partnerVendorRepository.save(entity);
        notificationService.notifyStatusChange(saved, status);
        return toResponse(saved);
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
        entity.setBusinessName(dto.getBusinessName());
        entity.setPartnerType(dto.getPartnerType());
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
        return response;
    }
}
