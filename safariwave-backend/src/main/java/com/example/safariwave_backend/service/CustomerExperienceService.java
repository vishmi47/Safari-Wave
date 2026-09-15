package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.ComplaintResponseRequestDTO;
import com.example.safariwave_backend.dto.request.ComplaintStatusUpdateRequestDTO;
import com.example.safariwave_backend.dto.request.CustomerExperienceRequestDTO;
import com.example.safariwave_backend.dto.response.CustomerExperienceResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerExperienceService {

    CustomerExperienceResponseDTO createExperience(CustomerExperienceRequestDTO dto);

    List<CustomerExperienceResponseDTO> getAllExperiences();

    Optional<CustomerExperienceResponseDTO> getExperienceById(Integer id);

    List<CustomerExperienceResponseDTO> getExperiencesByCustomer(Integer customerId);

    List<CustomerExperienceResponseDTO> getExperiencesByType(String experienceType);

    List<CustomerExperienceResponseDTO> getExperiencesByStatus(String status);

    CustomerExperienceResponseDTO updateExperience(Integer id, CustomerExperienceRequestDTO dto);

    CustomerExperienceResponseDTO updateComplaintStatus(Integer id, ComplaintStatusUpdateRequestDTO dto);

    CustomerExperienceResponseDTO updateComplaintResponse(Integer id, ComplaintResponseRequestDTO dto);

    void deleteExperience(Integer id);

    List<CustomerExperienceResponseDTO> getAllComplaints();

    Optional<CustomerExperienceResponseDTO> getComplaintById(Integer id);

    CustomerExperienceResponseDTO addComplaintNotes(Integer id, String notes);

    CustomerExperienceResponseDTO respondToComplaint(Integer id, ComplaintResponseRequestDTO dto);

    CustomerExperienceResponseDTO escalateComplaint(Integer id, String reason);

    List<CustomerExperienceResponseDTO> getComplaintsByStatus(String status);

    List<CustomerExperienceResponseDTO> getComplaintsByDateRange(LocalDateTime from, LocalDateTime to);

    CustomerExperienceResponseDTO updateComplaintStatusDirectly(Integer id, String status, String internalNotes);

    Object getDashboardSummary();
}
