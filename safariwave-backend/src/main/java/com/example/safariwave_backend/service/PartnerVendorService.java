package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.PartnerVendorRequestDTO;
import com.example.safariwave_backend.dto.response.PartnerVendorResponseDTO;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PartnerVendorService {

    PartnerVendorResponseDTO createPartnerVendor(PartnerVendorRequestDTO dto);

    List<PartnerVendorResponseDTO> getAllPartnerVendors();

    PartnerVendorResponseDTO getPartnerVendorById(Long id);

    List<PartnerVendorResponseDTO> getPartnerVendorsByStatus(PartnerVendorStatus status);

    /** Paged search; each role only sees the partner types it is allowed to (spec B.3). */
    Page<PartnerVendorResponseDTO> searchPartnerVendors(String query, String status, String type, int page, int size);

    PartnerVendorResponseDTO updatePartnerVendor(Long id, PartnerVendorRequestDTO dto);

    /** Marks the partner INACTIVE (soft delete). */
    default void deletePartnerVendor(Long id) {
        deletePartnerVendor(id, false);
    }

    /** permanent=true removes a partner that is already INACTIVE or TERMINATED; otherwise it becomes INACTIVE. */
    void deletePartnerVendor(Long id, boolean permanent);

    PartnerVendorResponseDTO approvePartnerVendor(Long id);

    PartnerVendorResponseDTO rejectPartnerVendor(Long id);

    PartnerVendorResponseDTO terminatePartnerVendor(Long id);

    /** Stores an agreement document privately and returns the URL to save in documentUrl. */
    String storeDocument(MultipartFile file);

    /** Loads a stored agreement document if the logged-in user may see the partner it belongs to. */
    Resource loadDocument(String filename);
}
