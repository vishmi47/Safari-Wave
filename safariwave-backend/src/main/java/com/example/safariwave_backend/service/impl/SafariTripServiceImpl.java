package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.SafariTripRequestDTO;
import com.example.safariwave_backend.dto.request.TripAssignmentRequestDTO;
import com.example.safariwave_backend.dto.response.SafariTripResponseDTO;
import com.example.safariwave_backend.dto.response.UserResponseDTO;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ConflictException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.EmergencyIncidentRepository;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.SafetyInspectionRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.NotificationService;
import com.example.safariwave_backend.service.SafariTripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Trip schedule for the Booking & Reservation module (spec 6.1 / B.3).
 *
 * OPERATIONS (and ADMIN) create trips, assign boat and guide, change status, suspend/cancel and delete unused trips.
 * The assigned GUIDE may move a trip to IN_PROGRESS or COMPLETED. Every logged-in user can read the schedule.
 * Status changes update the trip's bookings and notify the customers.
 */
@Service
public class SafariTripServiceImpl implements SafariTripService {

    static final String SCHEDULED = "SCHEDULED";
    static final String IN_PROGRESS = "IN_PROGRESS";
    static final String COMPLETED = "COMPLETED";
    static final String SUSPENDED = "SUSPENDED";
    static final String CANCELLED = "CANCELLED";
    private static final List<String> TRIP_STATUSES = List.of(SCHEDULED, IN_PROGRESS, COMPLETED, SUSPENDED, CANCELLED);
    private static final List<String> GUIDE_STATUSES = List.of(IN_PROGRESS, COMPLETED);
    private static final List<String> SEAT_HOLDING = List.of("PENDING", "CONFIRMED");

    @Autowired
    private SafariTripRepository safariTripRepository;

    @Autowired
    private SafariPackageRepository safariPackageRepository;

    @Autowired
    private BoatRepository boatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SafetyInspectionRepository safetyInspectionRepository;

    @Autowired
    private EmergencyIncidentRepository emergencyIncidentRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public SafariTripResponseDTO createTrip(SafariTripRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);

        SafariPackage safariPackage = findPackage(dto.getPackageId());
        if (!"ACTIVE".equals(safariPackage.getStatus())) {
            throw new BadRequestException("Trips can only be scheduled for active packages.");
        }
        Boat boat = findBoat(dto.getBoatId());
        requireAssignableBoat(boat);
        validateSchedule(dto, boat, 0);
        String status = requireTripStatus(dto.getStatus());
        if (COMPLETED.equals(status) || CANCELLED.equals(status)) {
            throw new BadRequestException("A new trip cannot start as " + status.toLowerCase(Locale.ROOT) + ".");
        }

        SafariTrip trip = new SafariTrip();
        trip.setSafariPackage(safariPackage);
        trip.setBoat(boat);
        trip.setGuide(dto.getGuideId() == null ? null : findGuide(dto.getGuideId()));
        trip.setTripDate(dto.getTripDate());
        trip.setStartTime(dto.getStartTime());
        trip.setEndTime(dto.getEndTime());
        trip.setCapacity(dto.getCapacity());
        trip.setStatus(status);
        trip.setRemarks(dto.getRemarks());
        trip.setCreatedBy(CurrentUser.id());

        SafariTrip saved = safariTripRepository.save(trip);
        notifyGuideAssigned(saved);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<SafariTripResponseDTO> getTripById(Integer tripId) {
        CurrentUser.get();
        return safariTripRepository.findById(tripId).map(this::toResponseDTO);
    }

    @Override
    @Transactional
    public List<SafariTripResponseDTO> getAllTrips() {
        CurrentUser.get();
        completeEndedTrips();
        return safariTripRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelayString = "${safariwave.trips.completion-check-ms:60000}")
    @Transactional
    public void completeEndedTrips() {
        LocalDateTime now = LocalDateTime.now();
        safariTripRepository.findByStatusInAndTripDateLessThanEqual(List.of(SCHEDULED, IN_PROGRESS), now.toLocalDate())
                .stream()
                .filter(trip -> trip.getEndTime() != null
                        && !LocalDateTime.of(trip.getTripDate(), trip.getEndTime()).isAfter(now))
                .forEach(trip -> {
                    applyStatus(trip, COMPLETED, null);
                    safariTripRepository.save(trip);
                });
    }

    @Override
    public List<SafariTripResponseDTO> getAssignedTrips() {
        if (!CurrentUser.hasAnyRole(Roles.GUIDE)) {
            throw new AccessDeniedException("Only guides have assigned trips.");
        }
        return safariTripRepository.findByGuide_UserIdOrderByTripDateAsc(CurrentUser.id())
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponseDTO> getGuides() {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        List<String> guideRoleNames = new ArrayList<>(Roles.aliasesOf(Roles.GUIDE));
        guideRoleNames.add(Roles.GUIDE);
        return userRepository.findByRole_RoleNameInOrderByFirstNameAsc(guideRoleNames)
                .stream()
                .map(user -> {
                    UserResponseDTO dto = new UserResponseDTO();
                    dto.setUserId(user.getUserId());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setEmail(user.getEmail());
                    dto.setRoleName(user.getRole().getRoleName());
                    dto.setStatus(user.getStatus());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SafariTripResponseDTO updateTrip(Integer tripId, SafariTripRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);

        SafariTrip existing = findTrip(tripId);
        requireNotFinal(existing);
        int bookedSeats = bookedSeats(existing);

        SafariPackage safariPackage = findPackage(dto.getPackageId());
        if (!safariPackage.getPackageId().equals(existing.getSafariPackage().getPackageId())
                && bookingRepository.existsBySafariTrip_TripId(tripId)) {
            throw new BadRequestException("The package of a trip that already has bookings cannot be changed.");
        }
        Boat boat = findBoat(dto.getBoatId());
        if (!boat.getBoatId().equals(existing.getBoat().getBoatId())) {
            requireAssignableBoat(boat);
        }
        validateSchedule(dto, boat, bookedSeats);
        String status = requireTripStatus(dto.getStatus());

        boolean scheduleChanged = !dto.getTripDate().equals(existing.getTripDate())
                || !dto.getStartTime().equals(existing.getStartTime());
        User previousGuide = existing.getGuide();

        existing.setSafariPackage(safariPackage);
        existing.setBoat(boat);
        existing.setGuide(dto.getGuideId() == null ? null : findGuide(dto.getGuideId()));
        existing.setTripDate(dto.getTripDate());
        existing.setStartTime(dto.getStartTime());
        existing.setEndTime(dto.getEndTime());
        existing.setCapacity(dto.getCapacity());
        existing.setRemarks(dto.getRemarks());
        syncBookingsWithTrip(existing, scheduleChanged);
        if (!status.equals(existing.getStatus())) {
            applyStatus(existing, status, dto.getRemarks());
        }

        SafariTrip updated = safariTripRepository.save(existing);
        if (updated.getGuide() != null && (previousGuide == null || !previousGuide.getUserId().equals(updated.getGuide().getUserId()))) {
            notifyGuideAssigned(updated);
        }
        return toResponseDTO(updated);
    }

    @Override
    @Transactional
    public SafariTripResponseDTO assignTrip(Integer tripId, TripAssignmentRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        if (dto.getBoatId() == null && dto.getGuideId() == null) {
            throw new BadRequestException("Choose a boat or a guide to assign.");
        }
        SafariTrip trip = findTrip(tripId);
        requireNotFinal(trip);

        if (dto.getBoatId() != null && !dto.getBoatId().equals(trip.getBoat().getBoatId())) {
            Boat boat = findBoat(dto.getBoatId());
            requireAssignableBoat(boat);
            if (boat.getCapacity() != null && trip.getCapacity() != null && trip.getCapacity() > boat.getCapacity()) {
                throw new BadRequestException("This boat holds " + boat.getCapacity() + " passengers but the trip capacity is "
                        + trip.getCapacity() + ". Lower the trip capacity first.");
            }
            trip.setBoat(boat);
            syncBookingsWithTrip(trip, false);
        }
        if (dto.getGuideId() != null) {
            trip.setGuide(findGuide(dto.getGuideId()));
        }

        SafariTrip saved = safariTripRepository.save(trip);
        if (dto.getGuideId() != null) {
            notifyGuideAssigned(saved);
        }
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public SafariTripResponseDTO updateTripStatus(Integer tripId, String status, String remarks) {
        SafariTrip trip = findTrip(tripId);
        String newStatus = requireTripStatus(status);

        boolean opsOrAdmin = CurrentUser.isAdmin() || CurrentUser.hasAnyRole(Roles.OPERATIONS);
        if (!opsOrAdmin) {
            boolean assignedGuide = CurrentUser.hasAnyRole(Roles.GUIDE) && trip.getGuide() != null
                    && CurrentUser.isOwner(trip.getGuide().getUserId());
            if (!assignedGuide) {
                throw new AccessDeniedException("Only the Operations Supervisor or the trip's assigned guide can change its status.");
            }
            if (!GUIDE_STATUSES.contains(newStatus)) {
                throw new AccessDeniedException("Guides can only mark a trip as in progress or completed.");
            }
        }

        applyStatus(trip, newStatus, remarks);
        return toResponseDTO(safariTripRepository.save(trip));
    }

    @Override
    @Transactional
    public SafariTripResponseDTO suspendTrip(Integer tripId, String remarks) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        SafariTrip trip = findTrip(tripId);
        applyStatus(trip, SUSPENDED, remarks);
        return toResponseDTO(safariTripRepository.save(trip));
    }

    @Override
    @Transactional
    public SafariTripResponseDTO cancelTrip(Integer tripId, String remarks) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        SafariTrip trip = findTrip(tripId);
        applyStatus(trip, CANCELLED, remarks);
        return toResponseDTO(safariTripRepository.save(trip));
    }

    @Override
    @Transactional
    public void deleteTrip(Integer tripId) {
        CurrentUser.requireAnyRole(Roles.OPERATIONS);
        SafariTrip trip = findTrip(tripId);
        if (bookingRepository.existsBySafariTrip_TripId(tripId)
                || safetyInspectionRepository.existsBySafariTrip_TripId(tripId)
                || emergencyIncidentRepository.existsBySafariTrip_TripId(tripId)) {
            throw new ConflictException("This trip has bookings or safety records. Cancel it instead of deleting it.");
        }
        safariTripRepository.delete(trip);
    }

    // ---- status changes and their effect on bookings ----

    private void applyStatus(SafariTrip trip, String newStatus, String remarks) {
        requireNotFinal(trip);
        String oldStatus = trip.getStatus() == null ? SCHEDULED : trip.getStatus().toUpperCase(Locale.ROOT);
        trip.setStatus(newStatus);
        if (remarks != null && !remarks.isBlank()) {
            trip.setRemarks(remarks.trim());
        }
        if (newStatus.equals(oldStatus)) {
            return;
        }

        String tripLabel = trip.getSafariPackage().getPackageName() + " on " + trip.getTripDate();
        String reason = remarks == null || remarks.isBlank() ? "" : " Reason: " + remarks.trim();
        List<Booking> active = bookingRepository.findBySafariTrip_TripIdAndBookingStatusIn(trip.getTripId(), SEAT_HOLDING);

        for (Booking booking : active) {
            String message = null;
            switch (newStatus) {
                case CANCELLED -> {
                    booking.setBookingStatus("CANCELLED");
                    message = "Your trip " + tripLabel + " was cancelled, so booking #" + booking.getBookingId() + " is cancelled."
                            + ("SUCCESS".equals(booking.getPaymentStatus()) ? " Your refund will be processed by our finance team." : "")
                            + reason;
                }
                case SUSPENDED -> message = "Your trip " + tripLabel + " (booking #" + booking.getBookingId()
                        + ") has been postponed. We will contact you with a new time." + reason;
                case COMPLETED -> {
                    if ("CONFIRMED".equals(booking.getBookingStatus())) {
                        booking.setBookingStatus("COMPLETED");
                        message = "Thank you for sailing with us on " + tripLabel + ". You can now rate your trip.";
                    }
                }
                case SCHEDULED -> {
                    if (SUSPENDED.equals(oldStatus)) {
                        message = "Your trip " + tripLabel + " (booking #" + booking.getBookingId() + ") is back on schedule.";
                    }
                }
                default -> {
                }
            }
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            if (message != null) {
                notificationService.create(booking.getCustomer().getUserId(), "TRIP_" + newStatus, message);
            }
        }
    }

    /** Keeps the trip's active bookings in line with its boat, date and time; notifies customers when the time moved. */
    private void syncBookingsWithTrip(SafariTrip trip, boolean notifyScheduleChange) {
        String timeSlot = trip.getEndTime() == null ? trip.getStartTime().toString() : trip.getStartTime() + "-" + trip.getEndTime();
        for (Booking booking : bookingRepository.findBySafariTrip_TripIdAndBookingStatusIn(trip.getTripId(), SEAT_HOLDING)) {
            booking.setBoatId(trip.getBoat().getBoatId());
            booking.setTripDate(trip.getTripDate());
            booking.setTimeSlot(timeSlot);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            if (notifyScheduleChange) {
                notificationService.create(booking.getCustomer().getUserId(), "TRIP_RESCHEDULED",
                        "Your trip for booking #" + booking.getBookingId() + " has moved to " + trip.getTripDate() + " at " + trip.getStartTime() + ".");
            }
        }
    }

    // ---- validation ----

    private void validateSchedule(SafariTripRequestDTO dto, Boat boat, int bookedSeats) {
        if (dto.getEndTime() != null && !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BadRequestException("End time must be after start time.");
        }
        if (boat.getCapacity() != null && dto.getCapacity() > boat.getCapacity()) {
            throw new BadRequestException("Trip capacity cannot exceed the boat capacity of " + boat.getCapacity() + ".");
        }
        if (dto.getCapacity() < bookedSeats) {
            throw new BadRequestException("Capacity cannot be lower than the " + bookedSeats + " seat(s) already booked.");
        }
    }

    /** Spec B.3 (Inventory): a boat under maintenance or retired cannot be assigned to a trip. */
    private void requireAssignableBoat(Boat boat) {
        if (!"OPERATIONAL".equalsIgnoreCase(boat.getStatus())) {
            throw new BadRequestException("Boat " + boat.getBoatName() + " is " + String.valueOf(boat.getStatus()).toLowerCase(Locale.ROOT).replace('_', ' ')
                    + " and cannot be assigned to a trip.");
        }
    }

    private void requireNotFinal(SafariTrip trip) {
        if (COMPLETED.equalsIgnoreCase(trip.getStatus()) || CANCELLED.equalsIgnoreCase(trip.getStatus())) {
            throw new BadRequestException("Completed or cancelled trips cannot be changed.");
        }
    }

    private String requireTripStatus(String status) {
        String normalized = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || !TRIP_STATUSES.contains(normalized)) {
            throw new BadRequestException("Trip status must be one of " + TRIP_STATUSES + ".");
        }
        return normalized;
    }

    private User findGuide(Integer guideId) {
        User guide = userRepository.findById(guideId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + guideId));
        if (!Roles.GUIDE.equals(Roles.canonical(guide.getRole().getRoleName()))) {
            throw new BadRequestException(guide.getFirstName() + " is not a guide.");
        }
        return guide;
    }

    private int bookedSeats(SafariTrip trip) {
        Long taken = bookingRepository.sumSeatsOnTrip(trip.getTripId(), SEAT_HOLDING, null);
        return taken == null ? 0 : taken.intValue();
    }

    private SafariTrip findTrip(Integer tripId) {
        return safariTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
    }

    private SafariPackage findPackage(Integer packageId) {
        return safariPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + packageId));
    }

    private Boat findBoat(Integer boatId) {
        return boatRepository.findById(boatId)
                .orElseThrow(() -> new ResourceNotFoundException("Boat not found with id: " + boatId));
    }

    private void notifyGuideAssigned(SafariTrip trip) {
        if (trip.getGuide() != null) {
            notificationService.create(trip.getGuide().getUserId(), "TRIP_ASSIGNED",
                    "You are the guide for " + trip.getSafariPackage().getPackageName() + " on " + trip.getTripDate()
                            + " at " + trip.getStartTime() + " (trip #" + trip.getTripId() + ").");
        }
    }

    private SafariTripResponseDTO toResponseDTO(SafariTrip trip) {
        SafariTripResponseDTO dto = new SafariTripResponseDTO();
        dto.setTripId(trip.getTripId());
        dto.setPackageId(trip.getSafariPackage().getPackageId());
        dto.setPackageName(trip.getSafariPackage().getPackageName());
        dto.setBoatId(trip.getBoat().getBoatId());
        dto.setBoatName(trip.getBoat().getBoatName());
        dto.setTripDate(trip.getTripDate());
        dto.setStartTime(trip.getStartTime());
        dto.setEndTime(trip.getEndTime());
        dto.setCapacity(trip.getCapacity());
        dto.setStatus(trip.getStatus());
        dto.setRemarks(trip.getRemarks());
        dto.setCreatedAt(trip.getCreatedAt());
        dto.setUpdatedAt(trip.getUpdatedAt());
        dto.setCreatedBy(trip.getCreatedBy());
        if (trip.getGuide() != null) {
            dto.setGuideId(trip.getGuide().getUserId());
            dto.setGuideName(trip.getGuide().getFirstName() + " " + trip.getGuide().getLastName());
        }
        int booked = trip.getTripId() == null ? 0 : bookedSeats(trip);
        dto.setBookedSeats(booked);
        dto.setAvailableSeats(trip.getCapacity() == null ? null : Math.max(0, trip.getCapacity() - booked));
        return dto;
    }
}
