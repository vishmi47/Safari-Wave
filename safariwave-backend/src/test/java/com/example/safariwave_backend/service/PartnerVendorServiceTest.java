package com.example.safariwave_backend.service;

import com.example.safariwave_backend.exception.DuplicateResourceException;
import com.example.safariwave_backend.repository.PartnerVendorRepository;
import com.example.safariwave_backend.service.impl.PartnerVendorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerVendorServiceTest {

    @Mock
    private PartnerVendorRepository partnerVendorRepository;

    @Mock
    private PartnerVendorNotificationService partnerVendorNotificationService;

    @InjectMocks
    private PartnerVendorServiceImpl partnerVendorService;

    private PartnerVendorRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new PartnerVendorRequestDTO();
        requestDTO.setBusinessName("Wild Horizon Travel");
        requestDTO.setPartnerType("TRAVEL_AGENCY");
        requestDTO.setContactPerson("Dinesh Perera");
        requestDTO.setEmail("dinesh@wildhorizon.com");
        requestDTO.setPhone("+94771234567");
        requestDTO.setAddress("Colombo 07");
        requestDTO.setServiceType("Safari Tour Packages");
        requestDTO.setServiceDescription("Luxury safari packages for tourists");
        requestDTO.setContractNumber("SV-2026-001");
        requestDTO.setContractStartDate(LocalDate.now());
        requestDTO.setContractEndDate(LocalDate.now().plusMonths(12));
        requestDTO.setAgreementDetails("12 month agreement");
        requestDTO.setDocumentUrl("/uploads/partner-vendors/test.pdf");
    }

    @Test
    void createPartnerVendor_shouldSetPendingApprovalAndSave() {
        when(partnerVendorRepository.existsByBusinessNameIgnoreCase(anyString())).thenReturn(false);
        when(partnerVendorRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(partnerVendorRepository.save(any(PartnerVendorEntity.class))).thenAnswer(invocation -> {
            PartnerVendorEntity entity = invocation.getArgument(0);
            entity.setPartnerVendorId(1L);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        PartnerVendorResponseDTO response = partnerVendorService.createPartnerVendor(requestDTO);

        assertNotNull(response);
        assertEquals(PartnerVendorStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals("Wild Horizon Travel", response.getBusinessName());
        verify(partnerVendorRepository).save(any(PartnerVendorEntity.class));
    }

    @Test
    void duplicateName_shouldThrowException() {
        when(partnerVendorRepository.existsByBusinessNameIgnoreCase("Wild Horizon Travel")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> partnerVendorService.createPartnerVendor(requestDTO)
        );

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void invalidContractDates_shouldThrowException() {
        requestDTO.setContractStartDate(LocalDate.now().plusMonths(2));
        requestDTO.setContractEndDate(LocalDate.now().plusMonths(1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> partnerVendorService.createPartnerVendor(requestDTO)
        );

        assertTrue(exception.getMessage().contains("contract start date"));
    }

    @Test
    void getPartnerVendorById_shouldReturnEntity() {
        PartnerVendorEntity entity = new PartnerVendorEntity();
        entity.setPartnerVendorId(9L);
        entity.setBusinessName("Ocean View Hotel");
        entity.setPartnerType("HOTEL");
        entity.setContactPerson("Nimal Silva");
        entity.setEmail("nimal@oceanview.com");
        entity.setPhone("+94770000000");
        entity.setAddress("Galle");
        entity.setServiceType("Accommodation");
        entity.setContractStartDate(LocalDate.now());
        entity.setContractEndDate(LocalDate.now().plusMonths(6));
        entity.setStatus(PartnerVendorStatus.PENDING_APPROVAL);

        when(partnerVendorRepository.findById(9L)).thenReturn(Optional.of(entity));

        PartnerVendorResponseDTO response = partnerVendorService.getPartnerVendorById(9L);

        assertNotNull(response);
        assertEquals("Ocean View Hotel", response.getBusinessName());
    }

    @Test
    void approvePartnerVendor_shouldUpdateStatus() {
        PartnerVendorEntity entity = new PartnerVendorEntity();
        entity.setPartnerVendorId(3L);
        entity.setBusinessName("Blue Wave Logistics");
        entity.setPartnerType("TRANSPORTATION_COMPANY");
        entity.setContactPerson("Asha Silva");
        entity.setEmail("asha@bluewave.com");
        entity.setPhone("+94778889999");
        entity.setAddress("Kandy");
        entity.setServiceType("Shuttle Services");
        entity.setStatus(PartnerVendorStatus.PENDING_APPROVAL);

        when(partnerVendorRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(partnerVendorRepository.save(any(PartnerVendorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartnerVendorResponseDTO response = partnerVendorService.approvePartnerVendor(3L);

        assertEquals(PartnerVendorStatus.APPROVED, response.getStatus());
        verify(partnerVendorRepository).save(any(PartnerVendorEntity.class));
    }

    @Test
    void getAllPartnerVendors_shouldReturnList() {
        PartnerVendorEntity entity = new PartnerVendorEntity();
        entity.setPartnerVendorId(5L);
        entity.setBusinessName("Forest Edge Restaurant");
        entity.setPartnerType("RESTAURANT");
        entity.setContactPerson("Kasun Perera");
        entity.setEmail("kasun@forestedge.com");
        entity.setPhone("+94776543210");
        entity.setAddress("Matara");
        entity.setServiceType("Dining");
        entity.setStatus(PartnerVendorStatus.APPROVED);

        when(partnerVendorRepository.findAll()).thenReturn(List.of(entity));

        List<PartnerVendorResponseDTO> vendors = partnerVendorService.getAllPartnerVendors();

        assertEquals(1, vendors.size());
        assertEquals("Forest Edge Restaurant", vendors.get(0).getBusinessName());
    }
}
