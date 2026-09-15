package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.ComplaintResponseRequestDTO;
import com.example.safariwave_backend.dto.request.ComplaintStatusUpdateRequestDTO;
import com.example.safariwave_backend.dto.request.CustomerExperienceRequestDTO;
import com.example.safariwave_backend.dto.response.CustomerExperienceResponseDTO;
import com.example.safariwave_backend.entity.*;
import com.example.safariwave_backend.enums.CustomerExperienceStatus;
import com.example.safariwave_backend.enums.CustomerExperienceType;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.CustomerExperienceRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.service.CustomerExperienceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerExperienceServiceImpl implements CustomerExperienceService {

    private final CustomerExperienceRepository customerExperienceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public CustomerExperienceServiceImpl(CustomerExperienceRepository customerExperienceRepository,
                                        UserRepository userRepository,
                                        BookingRepository bookingRepository) {
        this.customerExperienceRepository = customerExperienceRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public CustomerExperienceResponseDTO createExperience(CustomerExperienceRequestDTO dto) {
        validateRequest(dto);

        User customer = userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + dto.getCustomerId()));

        Booking booking = null;
        if (dto.getBookingId() != null) {
            booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + dto.getBookingId()));
        }

        CustomerExperience experience = new CustomerExperience();
        experience.setCustomer(customer);
        experience.setBooking(booking);
        experience.setExperienceType(parseExperienceType(dto.getExperienceType()));
        experience.setSubject(dto.getSubject());
        experience.setDescription(dto.getDescription());
        experience.setRating(dto.getRating());
        experience.setStatus(parseStatus(dto.getStatus(), "SUBMITTED"));
        experience.setResponse(dto.getResponse());
        experience.setInternalNotes(dto.getInternalNotes());

        if (experience.getStatus() == CustomerExperienceStatus.RESOLVED && (experience.getResponse() == null || experience.getResponse().trim().isEmpty())) {
            throw new RuntimeException("Resolution response is required when marking an experience as resolved.");
        }

        CustomerExperience saved = customerExperienceRepository.save(experience);
        return toResponseDTO(saved);
    }

    @Override
    public List<CustomerExperienceResponseDTO> getAllExperiences() {
        return customerExperienceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CustomerExperienceResponseDTO> getExperienceById(Integer id) {
        return customerExperienceRepository.findById(id).map(this::toResponseDTO);
    }

    @Override
    public List<CustomerExperienceResponseDTO> getExperiencesByCustomer(Integer customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + customerId));
        return customerExperienceRepository.findByCustomer(customer).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerExperienceResponseDTO> getExperiencesByType(String experienceType) {
        return customerExperienceRepository.findByExperienceType(parseExperienceType(experienceType)).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerExperienceResponseDTO> getExperiencesByStatus(String status) {
        return customerExperienceRepository.findByStatus(parseStatus(status, "SUBMITTED")).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerExperienceResponseDTO updateExperience(Integer id, CustomerExperienceRequestDTO dto) {
        CustomerExperience existing = customerExperienceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Experience not found with id: " + id));

        validateRequest(dto);

        if (dto.getCustomerId() != null) {
            User customer = userRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found with id: " + dto.getCustomerId()));
            existing.setCustomer(customer);
        }

        if (dto.getBookingId() != null) {
            Booking booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found with id: " + dto.getBookingId()));
            existing.setBooking(booking);
        }

        existing.setExperienceType(parseExperienceType(dto.getExperienceType()));
        existing.setSubject(dto.getSubject());
        existing.setDescription(dto.getDescription());
        existing.setRating(dto.getRating());
        existing.setStatus(parseStatus(dto.getStatus(), existing.getStatus() == null ? "SUBMITTED" : existing.getStatus().name()));
        existing.setResponse(dto.getResponse());
        existing.setInternalNotes(dto.getInternalNotes());

        return toResponseDTO(customerExperienceRepository.save(existing));
    }

    @Override
    public CustomerExperienceResponseDTO updateComplaintStatus(Integer id, ComplaintStatusUpdateRequestDTO dto) {
        CustomerExperience existing = customerExperienceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));

        existing.setStatus(parseStatus(dto.getStatus(), existing.getStatus().name()));
        if (dto.getInternalNotes() != null && !dto.getInternalNotes().trim().isEmpty()) {
            existing.setInternalNotes(dto.getInternalNotes());
        }

        return toResponseDTO(customerExperienceRepository.save(existing));
    }

    @Override
    public CustomerExperienceResponseDTO updateComplaintResponse(Integer id, ComplaintResponseRequestDTO dto) {
        CustomerExperience existing = customerExperienceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));

        existing.setResponse(dto.getResponse());
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            existing.setStatus(parseStatus(dto.getStatus(), existing.getStatus().name()));
        }

        return toResponseDTO(customerExperienceRepository.save(existing));
    }

    @Override
    public void deleteExperience(Integer id) {
        if (!customerExperienceRepository.existsById(id)) {
            throw new RuntimeException("Experience not found with id: " + id);
        }
        customerExperienceRepository.deleteById(id);
    }

    @Override
    public List<CustomerExperienceResponseDTO> getAllComplaints() {
        return customerExperienceRepository.findByExperienceType(CustomerExperienceType.COMPLAINT).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CustomerExperienceResponseDTO> getComplaintById(Integer id) {
        return customerExperienceRepository.findById(id)
                .filter(experience -> experience.getExperienceType() == CustomerExperienceType.COMPLAINT)
                .map(this::toResponseDTO);
    }

    @Override
    public CustomerExperienceResponseDTO addComplaintNotes(Integer id, String notes) {
        CustomerExperience existing = customerExperienceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));

        existing.setInternalNotes(notes);
        return toResponseDTO(customerExperienceRepository.save(existing));
    }

    @Override
    public CustomerExperienceResponseDTO respondToComplaint(Integer id, ComplaintResponseRequestDTO dto) {
        return updateComplaintResponse(id, dto);
    }

    @Override
    public CustomerExperienceResponseDTO escalateComplaint(Integer id, String reason) {
        CustomerExperience existing = customerExperienceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));

        existing.setStatus(CustomerExperienceStatus.ESCALATED);
        existing.setInternalNotes(reason != null ? reason : existing.getInternalNotes());
        return toResponseDTO(customerExperienceRepository.save(existing));
    }

    @Override
    public List<CustomerExperienceResponseDTO> getComplaintsByStatus(String status) {
        return customerExperienceRepository.findByExperienceTypeAndStatus(CustomerExperienceType.COMPLAINT, parseStatus(status, "SUBMITTED")).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerExperienceResponseDTO> getComplaintsByDateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new RuntimeException("Both from and to dates are required.");
        }
        return customerExperienceRepository.findByCreatedAtBetween(from, to).stream()
                .filter(experience -> experience.getExperienceType() == CustomerExperienceType.COMPLAINT)
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerExperienceResponseDTO updateComplaintStatusDirectly(Integer id, String status, String internalNotes) {
        ComplaintStatusUpdateRequestDTO dto = new ComplaintStatusUpdateRequestDTO();
        dto.setStatus(status);
        dto.setInternalNotes(internalNotes);
        return updateComplaintStatus(id, dto);
    }

    @Override
    public Object getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalExperiences", customerExperienceRepository.count());
        summary.put("totalComplaints", customerExperienceRepository.findByExperienceType(CustomerExperienceType.COMPLAINT).size());
        summary.put("submittedComplaints", customerExperienceRepository.findByStatus(CustomerExperienceStatus.SUBMITTED).size());
        summary.put("resolvedComplaints", customerExperienceRepository.findByStatus(CustomerExperienceStatus.RESOLVED).size());
        summary.put("escalatedComplaints", customerExperienceRepository.findByStatus(CustomerExperienceStatus.ESCALATED).size());
        return summary;
    }

    private void validateRequest(CustomerExperienceRequestDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Customer experience request is required.");
        }
        if (dto.getCustomerId() == null) {
            throw new RuntimeException("Customer is required.");
        }
        if (dto.getExperienceType() == null || dto.getExperienceType().trim().isEmpty()) {
            throw new RuntimeException("Experience type is required.");
        }
        if (dto.getSubject() == null || dto.getSubject().trim().isEmpty()) {
            throw new RuntimeException("Subject is required.");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new RuntimeException("Description is required.");
        }
    }

    private CustomerExperienceType parseExperienceType(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Experience type is required.");
        }
        try {
            return CustomerExperienceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid experience type: " + value);
        }
    }

    private CustomerExperienceStatus parseStatus(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return CustomerExperienceStatus.valueOf(defaultValue.trim().toUpperCase());
        }
        try {
            return CustomerExperienceStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid status: " + value);
        }
    }

    private CustomerExperienceResponseDTO toResponseDTO(CustomerExperience experience) {
        CustomerExperienceResponseDTO dto = new CustomerExperienceResponseDTO();
        dto.setId(experience.getId());
        dto.setCustomerId(experience.getCustomer() != null ? experience.getCustomer().getUserId() : null);
        dto.setCustomerName(experience.getCustomer() != null
                ? experience.getCustomer().getFirstName() + " " + experience.getCustomer().getLastName() : null);
        dto.setBookingId(experience.getBooking() != null ? experience.getBooking().getBookingId() : null);
        dto.setExperienceType(experience.getExperienceType() != null ? experience.getExperienceType().name() : null);
        dto.setSubject(experience.getSubject());
        dto.setDescription(experience.getDescription());
        dto.setRating(experience.getRating());
        dto.setStatus(experience.getStatus() != null ? experience.getStatus().name() : null);
        dto.setResponse(experience.getResponse());
        dto.setInternalNotes(experience.getInternalNotes());
        dto.setCreatedAt(experience.getCreatedAt());
        dto.setUpdatedAt(experience.getUpdatedAt());
        dto.setResolvedAt(experience.getResolvedAt());
        return dto;
    }
}
