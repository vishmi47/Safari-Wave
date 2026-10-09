package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.ComplaintResponseRequestDTO;
import com.example.safariwave_backend.dto.request.ComplaintStatusUpdateRequestDTO;
import com.example.safariwave_backend.dto.request.CustomerExperienceRequestDTO;
import com.example.safariwave_backend.dto.response.CustomerExperienceResponseDTO;
import com.example.safariwave_backend.entity.*;
import com.example.safariwave_backend.enums.CustomerExperienceStatus;
import com.example.safariwave_backend.enums.CustomerExperienceType;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.CustomerExperienceRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.CustomerExperienceService;
import com.example.safariwave_backend.service.NotificationService;
import com.example.safariwave_backend.service.support.PagingSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Customer Experience (spec 6.5 / B.3).
 *
 * CUSTOMER: submit feedback, ratings, complaints, surveys and loyalty enrolments; read own; edit or delete own
 * until staff respond. A rating (1-5) must belong to the customer's own COMPLETED booking.
 * CUSTOMER_RELATIONS: read all; update status, responses and notes; delete invalid (rejected) or resolved records.
 * MARKETING: ratings and aggregate stats (customer details hidden). GUIDE: feedback on trips they guided.
 * ADMIN: everything. Internal notes are never shown to customers, marketing or guides.
 */
@Service
public class CustomerExperienceServiceImpl implements CustomerExperienceService {

    private static final List<CustomerExperienceStatus> OPEN_STATUSES = List.of(CustomerExperienceStatus.SUBMITTED, CustomerExperienceStatus.PENDING);
    private static final List<CustomerExperienceStatus> DELETABLE_BY_STAFF = List.of(CustomerExperienceStatus.RESOLVED, CustomerExperienceStatus.REJECTED);
    private static final List<CustomerExperienceType> TRIP_FEEDBACK_TYPES = List.of(CustomerExperienceType.FEEDBACK, CustomerExperienceType.RATING);

    private final CustomerExperienceRepository customerExperienceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public CustomerExperienceServiceImpl(CustomerExperienceRepository customerExperienceRepository,
                                        UserRepository userRepository,
                                        BookingRepository bookingRepository,
                                        NotificationService notificationService) {
        this.customerExperienceRepository = customerExperienceRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @Override
    public CustomerExperienceResponseDTO createExperience(CustomerExperienceRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.CUSTOMER);
        validateRequest(dto);
        boolean customer = isCustomer();
        // Customers always submit for themselves; ADMIN may record one for a named customer
        Integer customerId = customer ? CurrentUser.id() : dto.getCustomerId();
        if (customerId == null) {
            throw new BadRequestException("Customer is required.");
        }
        User customerUser = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        CustomerExperience experience = new CustomerExperience();
        experience.setCustomer(customerUser);
        experience.setExperienceType(parseExperienceType(dto.getExperienceType()));
        experience.setSubject(dto.getSubject().trim());
        experience.setDescription(dto.getDescription().trim());
        applyBookingAndRating(experience, dto, customerId);
        if (customer) {
            experience.setStatus(CustomerExperienceStatus.SUBMITTED);
        } else {
            experience.setStatus(parseStatus(dto.getStatus(), "SUBMITTED"));
            experience.setResponse(dto.getResponse());
            experience.setInternalNotes(dto.getInternalNotes());
        }

        if (experience.getStatus() == CustomerExperienceStatus.RESOLVED && (experience.getResponse() == null || experience.getResponse().trim().isEmpty())) {
            throw new BadRequestException("Resolution response is required when marking an experience as resolved.");
        }

        CustomerExperience saved = customerExperienceRepository.save(experience);
        return toResponseDTO(saved);
    }

    @Override
    public List<CustomerExperienceResponseDTO> getAllExperiences() {
        requireRelations();
        return customerExperienceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CustomerExperienceResponseDTO> getExperienceById(Integer id) {
        Optional<CustomerExperience> experience = customerExperienceRepository.findById(id);
        experience.ifPresent(this::requireCanView);
        return experience.map(this::toResponseDTO);
    }

    @Override
    public List<CustomerExperienceResponseDTO> getExperiencesByCustomer(Integer customerId) {
        CurrentUser.requireOwnerOrAnyRole(customerId, Roles.CUSTOMER_RELATIONS);
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        return customerExperienceRepository.findByCustomer(customer).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerExperienceResponseDTO> getExperiencesByType(String experienceType) {
        requireRelations();
        return customerExperienceRepository.findByExperienceType(parseExperienceType(experienceType)).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerExperienceResponseDTO> getExperiencesByStatus(String status) {
        requireRelations();
        return customerExperienceRepository.findByStatus(parseStatus(status, "SUBMITTED")).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerExperienceResponseDTO updateExperience(Integer id, CustomerExperienceRequestDTO dto) {
        CustomerExperience existing = findExperience(id);
        validateRequest(dto);

        if (isCustomer()) {
            // Customers change their own record only until staff respond (spec B.3)
            requireOwnOpenRecord(existing, "edit");
            existing.setExperienceType(parseExperienceType(dto.getExperienceType()));
            existing.setSubject(dto.getSubject().trim());
            existing.setDescription(dto.getDescription().trim());
            applyBookingAndRating(existing, dto, existing.getCustomer().getUserId());
            return toResponseDTO(customerExperienceRepository.save(existing));
        }

        if (CurrentUser.isAdmin()) {
            if (dto.getCustomerId() != null) {
                User customer = userRepository.findById(dto.getCustomerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + dto.getCustomerId()));
                existing.setCustomer(customer);
            }
            if (dto.getBookingId() != null) {
                Booking booking = bookingRepository.findById(dto.getBookingId())
                        .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + dto.getBookingId()));
                existing.setBooking(booking);
            }
            existing.setExperienceType(parseExperienceType(dto.getExperienceType()));
            existing.setSubject(dto.getSubject());
            existing.setDescription(dto.getDescription());
            existing.setRating(dto.getRating());
        } else {
            // Customer relations: status, response and notes only
            requireRelations();
        }
        existing.setStatus(parseStatus(dto.getStatus(), existing.getStatus() == null ? "SUBMITTED" : existing.getStatus().name()));
        existing.setResponse(dto.getResponse());
        existing.setInternalNotes(dto.getInternalNotes());
        return toResponseDTO(customerExperienceRepository.save(existing));
    }

    @Override
    public CustomerExperienceResponseDTO updateComplaintStatus(Integer id, ComplaintStatusUpdateRequestDTO dto) {
        requireRelations();
        CustomerExperience existing = findExperience(id);
        existing.setStatus(parseStatus(dto.getStatus(), existing.getStatus().name()));
        if (dto.getInternalNotes() != null && !dto.getInternalNotes().trim().isEmpty()) {
            existing.setInternalNotes(dto.getInternalNotes());
        }
        CustomerExperience saved = customerExperienceRepository.save(existing);
        notificationService.create(saved.getCustomer().getUserId(), "COMPLAINT_UPDATED", "Your complaint status was updated");
        return toResponseDTO(saved);
    }

    @Override
    public CustomerExperienceResponseDTO updateComplaintResponse(Integer id, ComplaintResponseRequestDTO dto) {
        requireRelations();
        CustomerExperience existing = findExperience(id);
        if (dto.getResponse() == null || dto.getResponse().isBlank()) {
            throw new BadRequestException("Response is required.");
        }
        existing.setResponse(dto.getResponse().trim());
        existing.setStatus(dto.getStatus() != null && !dto.getStatus().trim().isEmpty()
                ? parseStatus(dto.getStatus(), existing.getStatus().name())
                : CustomerExperienceStatus.RESPONDED);
        CustomerExperience saved = customerExperienceRepository.save(existing);
        notificationService.create(saved.getCustomer().getUserId(), "COMPLAINT_RESPONSE", "Staff responded to your " + saved.getExperienceType().name().toLowerCase().replace('_', ' '));
        return toResponseDTO(saved);
    }

    @Override
    public void deleteExperience(Integer id) {
        CustomerExperience existing = findExperience(id);
        if (isCustomer()) {
            requireOwnOpenRecord(existing, "delete");
        } else if (!CurrentUser.isAdmin()) {
            requireRelations();
            if (!DELETABLE_BY_STAFF.contains(existing.getStatus())) {
                throw new BadRequestException("Only resolved or rejected (invalid) records can be deleted.");
            }
        }
        customerExperienceRepository.delete(existing);
    }

    @Override
    public List<CustomerExperienceResponseDTO> getAllComplaints() {
        requireRelations();
        return customerExperienceRepository.findByExperienceType(CustomerExperienceType.COMPLAINT).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CustomerExperienceResponseDTO> getComplaintById(Integer id) {
        requireRelations();
        return customerExperienceRepository.findById(id)
                .filter(experience -> experience.getExperienceType() == CustomerExperienceType.COMPLAINT)
                .map(this::toResponseDTO);
    }

    @Override
    public CustomerExperienceResponseDTO addComplaintNotes(Integer id, String notes) {
        requireRelations();
        CustomerExperience existing = findExperience(id);
        existing.setInternalNotes(notes);
        return toResponseDTO(customerExperienceRepository.save(existing));
    }

    @Override
    public CustomerExperienceResponseDTO respondToComplaint(Integer id, ComplaintResponseRequestDTO dto) {
        return updateComplaintResponse(id, dto);
    }

    @Override
    public CustomerExperienceResponseDTO escalateComplaint(Integer id, String reason) {
        requireRelations();
        CustomerExperience existing = findExperience(id);
        existing.setStatus(CustomerExperienceStatus.ESCALATED);
        existing.setInternalNotes(reason != null ? reason : existing.getInternalNotes());
        return toResponseDTO(customerExperienceRepository.save(existing));
    }

    @Override
    public List<CustomerExperienceResponseDTO> getComplaintsByStatus(String status) {
        requireRelations();
        return customerExperienceRepository.findByExperienceTypeAndStatus(CustomerExperienceType.COMPLAINT, parseStatus(status, "SUBMITTED")).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerExperienceResponseDTO> getComplaintsByDateRange(LocalDateTime from, LocalDateTime to) {
        requireRelations();
        if (from == null || to == null) {
            throw new BadRequestException("Both from and to dates are required.");
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
        CurrentUser.requireAnyRole(Roles.CUSTOMER_RELATIONS, Roles.MARKETING);
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalExperiences", customerExperienceRepository.count());
        summary.put("totalComplaints", customerExperienceRepository.findByExperienceType(CustomerExperienceType.COMPLAINT).size());
        summary.put("submittedComplaints", customerExperienceRepository.findByStatus(CustomerExperienceStatus.SUBMITTED).size());
        summary.put("resolvedComplaints", customerExperienceRepository.findByStatus(CustomerExperienceStatus.RESOLVED).size());
        summary.put("escalatedComplaints", customerExperienceRepository.findByStatus(CustomerExperienceStatus.ESCALATED).size());
        return summary;
    }

    @Override
    public Page<CustomerExperienceResponseDTO> searchExperiences(String query, String status, String type, int page, int size) {
        Integer customerId = null;
        Integer guideId = null;
        boolean ratedOnly = false;
        List<CustomerExperienceType> types = type == null || type.isBlank()
                ? Arrays.asList(CustomerExperienceType.values())
                : List.of(parseExperienceType(type));
        if (isCustomer()) {
            customerId = CurrentUser.id();
        } else if (CurrentUser.isAdmin() || CurrentUser.hasAnyRole(Roles.CUSTOMER_RELATIONS)) {
            // all records
        } else if (CurrentUser.hasAnyRole(Roles.MARKETING)) {
            ratedOnly = true;
        } else if (CurrentUser.hasAnyRole(Roles.GUIDE)) {
            guideId = CurrentUser.id();
            types = types.stream().filter(TRIP_FEEDBACK_TYPES::contains).collect(Collectors.toList());
            if (types.isEmpty()) {
                throw new AccessDeniedException("Guides can only see feedback and ratings for their trips.");
            }
        } else {
            throw new AccessDeniedException("You are not authorized to view customer experience records.");
        }
        CustomerExperienceStatus statusFilter = status == null || status.isBlank() ? null : parseStatus(status, null);
        return customerExperienceRepository.search(PagingSupport.text(query), statusFilter, types, customerId, guideId, ratedOnly,
                        PagingSupport.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponseDTO);
    }

    @Override
    public List<CustomerExperienceResponseDTO> getRatings() {
        CurrentUser.requireAnyRole(Roles.MARKETING, Roles.CUSTOMER_RELATIONS);
        return customerExperienceRepository.findByRatingIsNotNull().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getRatingSummary() {
        CurrentUser.requireAnyRole(Roles.MARKETING, Roles.CUSTOMER_RELATIONS);
        List<CustomerExperience> rated = customerExperienceRepository.findByRatingIsNotNull();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("ratingCount", rated.size());
        summary.put("averageRating", rated.isEmpty() ? null : BigDecimal.valueOf(rated.stream().mapToInt(CustomerExperience::getRating).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP));
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            int value = stars;
            distribution.put(stars, rated.stream().filter(experience -> experience.getRating() == value).count());
        }
        summary.put("distribution", distribution);
        Map<String, Double> byPackage = rated.stream()
                .filter(experience -> experience.getBooking() != null && experience.getBooking().getSafariPackage() != null)
                .collect(Collectors.groupingBy(experience -> experience.getBooking().getSafariPackage().getPackageName(),
                        LinkedHashMap::new, Collectors.averagingInt(CustomerExperience::getRating)));
        summary.put("averageByPackage", byPackage);
        return summary;
    }

    @Override
    public List<CustomerExperienceResponseDTO> getFeedbackForMyTrips() {
        if (!CurrentUser.hasAnyRole(Roles.GUIDE)) {
            throw new AccessDeniedException("Only guides have trip feedback.");
        }
        return customerExperienceRepository.findByBooking_SafariTrip_Guide_UserIdAndExperienceTypeIn(CurrentUser.id(), TRIP_FEEDBACK_TYPES).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ---- rules ----

    /** A booking must be the customer's own; a rating needs a COMPLETED booking (spec B.3). */
    private void applyBookingAndRating(CustomerExperience experience, CustomerExperienceRequestDTO dto, Integer customerId) {
        Booking booking = null;
        if (dto.getBookingId() != null) {
            booking = bookingRepository.findById(dto.getBookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + dto.getBookingId()));
            if (booking.getCustomer() == null || !booking.getCustomer().getUserId().equals(customerId)) {
                throw new AccessDeniedException("You can only give feedback on your own bookings.");
            }
        }
        boolean ratingType = experience.getExperienceType() == CustomerExperienceType.RATING;
        if (ratingType && dto.getRating() == null) {
            throw new BadRequestException("A rating from 1 to 5 is required.");
        }
        if (dto.getRating() != null) {
            if (dto.getRating() < 1 || dto.getRating() > 5) {
                throw new BadRequestException("Rating must be between 1 and 5.");
            }
            if (booking == null || !"COMPLETED".equals(booking.getBookingStatus())) {
                throw new BadRequestException("You can only rate a trip you have completed.");
            }
        }
        experience.setBooking(booking);
        experience.setRating(dto.getRating());
    }

    private void requireOwnOpenRecord(CustomerExperience experience, String action) {
        if (!CurrentUser.isOwner(experience.getCustomer().getUserId())) {
            throw new AccessDeniedException("You can only " + action + " your own records.");
        }
        boolean responded = experience.getResponse() != null && !experience.getResponse().isBlank();
        if (responded || !OPEN_STATUSES.contains(experience.getStatus())) {
            throw new BadRequestException("This record can no longer be changed because our team has already responded.");
        }
    }

    private void requireCanView(CustomerExperience experience) {
        if (CurrentUser.isAdmin() || CurrentUser.hasAnyRole(Roles.CUSTOMER_RELATIONS)) {
            return;
        }
        if (isCustomer() && CurrentUser.isOwner(experience.getCustomer().getUserId())) {
            return;
        }
        if (CurrentUser.hasAnyRole(Roles.MARKETING) && experience.getRating() != null) {
            return;
        }
        Booking booking = experience.getBooking();
        if (CurrentUser.hasAnyRole(Roles.GUIDE) && TRIP_FEEDBACK_TYPES.contains(experience.getExperienceType())
                && booking != null && booking.getSafariTrip() != null && booking.getSafariTrip().getGuide() != null
                && CurrentUser.isOwner(booking.getSafariTrip().getGuide().getUserId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to view this record.");
    }

    private void requireRelations() {
        CurrentUser.requireAnyRole(Roles.CUSTOMER_RELATIONS);
    }

    private boolean isCustomer() {
        return CurrentUser.hasAnyRole(Roles.CUSTOMER);
    }

    private CustomerExperience findExperience(Integer id) {
        return customerExperienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found with id: " + id));
    }

    private void validateRequest(CustomerExperienceRequestDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Customer experience request is required.");
        }
        if (dto.getExperienceType() == null || dto.getExperienceType().trim().isEmpty()) {
            throw new BadRequestException("Experience type is required.");
        }
        if (dto.getSubject() == null || dto.getSubject().trim().isEmpty()) {
            throw new BadRequestException("Subject is required.");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            throw new BadRequestException("Description is required.");
        }
    }

    private CustomerExperienceType parseExperienceType(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException("Experience type is required.");
        }
        try {
            return CustomerExperienceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid experience type: " + value);
        }
    }

    private CustomerExperienceStatus parseStatus(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return CustomerExperienceStatus.valueOf(defaultValue.trim().toUpperCase());
        }
        try {
            return CustomerExperienceStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status: " + value);
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

        // Internal notes are for customer relations only; marketing and guides do not see who wrote the record
        boolean relationsView = CurrentUser.isAdmin() || CurrentUser.hasAnyRole(Roles.CUSTOMER_RELATIONS);
        if (!relationsView) {
            dto.setInternalNotes(null);
            if (!isCustomer()) {
                dto.setCustomerId(null);
                dto.setCustomerName(null);
            }
        }
        return dto;
    }
}
