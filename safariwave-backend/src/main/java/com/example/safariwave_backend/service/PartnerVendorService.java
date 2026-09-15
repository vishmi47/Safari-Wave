package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.PartnerVendorRequestDTO;
import com.example.safariwave_backend.dto.response.PartnerVendorResponseDTO;
import com.example.safariwave_backend.enums.PartnerVendorStatus;

import java.util.List;

public interface PartnerVendorService {

    PartnerVendorResponseDTO createPartnerVendor(PartnerVendorRequestDTO dto);

    List<PartnerVendorResponseDTO> getAllPartnerVendors();

    PartnerVendorResponseDTO getPartnerVendorById(Long id);

    List<PartnerVendorResponseDTO> getPartnerVendorsByStatus(PartnerVendorStatus status);

    PartnerVendorResponseDTO updatePartnerVendor(Long id, PartnerVendorRequestDTO dto);

    void deletePartnerVendor(Long id);

    PartnerVendorResponseDTO approvePartnerVendor(Long id);

    PartnerVendorResponseDTO rejectPartnerVendor(Long id);
}
