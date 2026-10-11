package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.BookingRequestDTO;
import com.example.safariwave_backend.dto.response.BookingResponseDTO;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.entity.SafariTrip;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ConflictException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.CustomerExperienceRepository;
import com.example.safariwave_backend.repository.PaymentRepository;
import com.example.safariwave_backend.repository.SafariTripRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.BookingService;
import com.example.safariwave_backend.service.NotificationService;
import com.example.safariwave_backend.service.pricing.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Booking & Reservation (spec 6.1 / B.3).
 *
 * CUSTOMER: create, read own, change own (trip/passengers) and cancel own until the cutoff before the trip.
 * CUSTOMER_RELATIONS: create on behalf of a customer, read all, update all, cancel.
 * OPERATIONS: read all (boat/guide assignment and trip status live in SafariTripServiceImpl).
 * FINANCE: read all, update payment/refund status only.
 * GUIDE: read bookings on assigned trips (trip status lives in SafariTripServiceImpl).
 * ADMIN: everything, including hard delete.
 */
@Service
public class BookingServiceImpl implements BookingService {

    static final String PENDING = "PENDING";
    static final String CONFIRMED = "CONFIRMED";
    static final String CANCELLED = "CANCELLED";
    static final String COMPLETED = "COMPLETED";
    /** Bookings in these statuses hold seats on their trip. */
    static final List<String> SEAT_HOLDING = List.of(PENDING, CONFIRMED);

    private static final List<String> BOOKING_STATUSES = List.of(PENDING, CONFIRMED, CANCELLED, COMPLETED);
    // SUCCESS is the existing "paid" value written by the mock payment flow and used by reports
    private static final List<String> PAYMENT_STATUSES = List.of("PENDING", "SUCCESS", "FAILED", "REFUNDED");
    private static final List<String> UNBOOKABLE_TRIP_STATUSES = List.of("IN_PROGRESS", "COMPLETED", "SUSPENDED", "CANCELLED");
    private static final String[] READ_ALL_ROLES = {Roles.CUSTOMER_RELATIONS, Roles.OPERATIONS, Roles.FINANCE};
    private static final int MAX_PAGE_SIZE = 100;

    /** Customers may change or cancel a booking until this many hours before the trip starts. */
    @Value("${safariwave.booking.cutoff-hours:24}")
    private long cutoffHours = 24;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SafariTripRepository safariTripRepository;

    /** Strategy pattern context: works out discounts and totals (see service/pricing). */
    @Autowired
    private PricingService pricingService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerExperienceRepository customerExperienceRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        CurrentUser.requireAnyRole(Roles.CUSTOMER, Roles.CUSTOMER_RELATIONS);
        boolean customer = isCustomer();

        Integer customerId = customer ? CurrentUser.id() : dto.getUserId();
        if (customerId == null) {
            throw new BadRequestException("Customer (userId) is required when booking on behalf of a customer.");
        }
        User customerUser = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + customerId));

        SafariTrip trip = findTrip(dto.getTripId());
        requireBookableTrip(trip);
        requireRequestMatchesTrip(dto, trip);
        if (!"ACTIVE".equals(trip.getSafariPackage().getStatus())) {
            throw new BadRequestException("This package is not available for booking.");
        }
        validateCapacity(trip, dto.getNumberOfPeople(), null);

        Booking booking = new Booking();
        booking.setCustomer(customerUser);
        applyTrip(booking, trip);
        booking.setNumberOfPeople(dto.getNumberOfPeople());
        snapshotPrice(booking, trip.getSafariPackage());
        booking.setTotalPrice(calculateTotal(booking));
        booking.setPaymentStatus(PENDING);
        booking.setBookingStatus(PENDING);
        booking.setCreatedBy(CurrentUser.id());
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        if (!customer) {
            notifyCustomer(saved, "BOOKING_CREATED",
                    "Booking #" + saved.getBookingId() + " for " + trip.getTripDate() + " was created for you by our team.");
        }
        return toResponseDTO(saved);
    }

    @Override
    public Optional<BookingResponseDTO> getBookingById(Integer bookingId) {
        Optional<Booking> booking = bookingRepository.findById(bookingId);
        booking.ifPresent(this::requireCanView);
        return booking.map(this::toResponseDTO);
    }

    @Override
    public List<BookingResponseDTO> getAllBookings() {
        List<Booking> bookings;
        if (CurrentUser.isAdmin() || CurrentUser.hasAnyRole(READ_ALL_ROLES)) {
            bookings = bookingRepository.findAll();
        } else if (isCustomer()) {
            bookings = bookingRepository.findByCustomer_UserId(CurrentUser.id());
        } else if (CurrentUser.hasAnyRole(Roles.GUIDE)) {
            bookings = bookingRepository.findBySafariTrip_Guide_UserId(CurrentUser.id());
        } else {
            throw new AccessDeniedException("You are not authorized to view bookings.");
        }
        return bookings.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDTO> getBookingsByUserId(Integer userId) {
        CurrentUser.requireOwnerOrAnyRole(userId, READ_ALL_ROLES);
        return bookingRepository.findByCustomer_UserId(userId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDTO> getBookingsByStatus(String bookingStatus) {
        CurrentUser.requireAnyRole(READ_ALL_ROLES);
        return bookingRepository.findByBookingStatus(normalize(bookingStatus))
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<BookingResponseDTO> searchBookings(String query, String status, int page, int size) {
        validatePaging(page, size);
        Integer customerId = null;
        Integer guideId = null;
        if (!CurrentUser.isAdmin() && !CurrentUser.hasAnyRole(READ_ALL_ROLES)) {
            if (isCustomer()) {
                customerId = CurrentUser.id();
            } else if (CurrentUser.hasAnyRole(Roles.GUIDE)) {
                guideId = CurrentUser.id();
            } else {
                throw new AccessDeniedException("You are not authorized to view bookings.");
            }
        }
        String statusFilter = normalize(status);
        if (statusFilter != null && !BOOKING_STATUSES.contains(statusFilter)) {
            throw new BadRequestException("Status must be one of " + BOOKING_STATUSES + ".");
        }
        String text = query == null || query.isBlank() ? null : query.trim();

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookingId"));
        return bookingRepository.search(text, statusFilter, customerId, guideId, pageRequest).map(this::toResponseDTO);
    }

    @Override
    @Transactional
    public BookingResponseDTO updateBooking(Integer bookingId, BookingRequestDTO dto) {
        Booking booking = findBooking(bookingId);
        boolean customer = isCustomer();
        if (customer) {
            requireOwnBooking(booking);
        } else {
            CurrentUser.requireAnyRole(Roles.CUSTOMER_RELATIONS);
        }
        if (!SEAT_HOLDING.contains(booking.getBookingStatus())) {
            throw new BadRequestException("Only pending or confirmed bookings can be changed.");
        }
        if (customer) {
            requireBeforeCutoff(tripStart(booking));
        }

        SafariTrip currentTrip = booking.getSafariTrip();
        boolean tripChanged = currentTrip == null || !currentTrip.getTripId().equals(dto.getTripId());
        SafariTrip newTrip = tripChanged ? findTrip(dto.getTripId()) : currentTrip;
        if (tripChanged) {
            requireBookableTrip(newTrip);
            if (customer) {
                requireBeforeCutoff(newTrip.getTripDate().atTime(newTrip.getStartTime()));
                if (booking.getSafariPackage() != null
                        && !booking.getSafariPackage().getPackageId().equals(newTrip.getSafariPackage().getPackageId())) {
                    throw new BadRequestException("You can only move a booking to another trip of the same package.");
                }
            }
        }
        // Re-check seats whenever the trip or passenger count changes (spec B.3); this booking's own seats don't count
        validateCapacity(newTrip, dto.getNumberOfPeople(), booking.getBookingId());

        boolean packageChanged = booking.getSafariPackage() == null
                || !booking.getSafariPackage().getPackageId().equals(newTrip.getSafariPackage().getPackageId());
        if (packageChanged) {
            snapshotPrice(booking, newTrip.getSafariPackage());
        } else if (booking.getUnitPrice() == null) {
            keepLegacyPrice(booking);
        }
        applyTrip(booking, newTrip);
        booking.setNumberOfPeople(dto.getNumberOfPeople());
        booking.setTotalPrice(calculateTotal(booking));
        booking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        if (!customer) {
            notifyCustomer(updated, "BOOKING_UPDATED", "Booking #" + updated.getBookingId() + " was updated by our team.");
        }
        return toResponseDTO(updated);
    }

    @Override
    @Transactional
    public BookingResponseDTO updateBookingStatus(Integer bookingId, String bookingStatus) {
        CurrentUser.requireAnyRole(Roles.CUSTOMER_RELATIONS);
        String status = normalize(bookingStatus);
        if (status == null || !BOOKING_STATUSES.contains(status)) {
            throw new BadRequestException("Booking status must be one of " + BOOKING_STATUSES + ".");
        }
        Booking booking = findBooking(bookingId);
        // Re-activating a cancelled booking takes seats again
        if (SEAT_HOLDING.contains(status) && !SEAT_HOLDING.contains(booking.getBookingStatus()) && booking.getSafariTrip() != null) {
            validateCapacity(booking.getSafariTrip(), booking.getNumberOfPeople(), booking.getBookingId());
        }
        booking.setBookingStatus(status);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking updated = bookingRepository.save(booking);
        notifyCustomer(updated, "BOOKING_" + status, "Booking #" + updated.getBookingId() + " is now " + status.toLowerCase(Locale.ROOT) + ".");
        return toResponseDTO(updated);
    }

    @Override
    @Transactional
    public BookingResponseDTO cancelBooking(Integer bookingId, String reason) {
        Booking booking = findBooking(bookingId);
        boolean customer = isCustomer();
        if (customer) {
            requireOwnBooking(booking);
        } else {
            CurrentUser.requireAnyRole(Roles.CUSTOMER_RELATIONS);
        }
        if (!SEAT_HOLDING.contains(booking.getBookingStatus())) {
            throw new BadRequestException("Only pending or confirmed bookings can be cancelled.");
        }
        if (customer) {
            requireBeforeCutoff(tripStart(booking));
        }

        booking.setBookingStatus(CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking cancelled = bookingRepository.save(booking);

        String message = "Booking #" + cancelled.getBookingId() + " has been cancelled.";
        if ("SUCCESS".equals(cancelled.getPaymentStatus())) {
            message += " Your refund will be processed by our finance team.";
        }
        if (reason != null && !reason.isBlank()) {
            message += " Reason: " + reason.trim();
        }
        notifyCustomer(cancelled, "BOOKING_CANCELLED", message);
        return toResponseDTO(cancelled);
    }

    @Override
    @Transactional
    public BookingResponseDTO updatePaymentStatus(Integer bookingId, String paymentStatus, String remarks) {
        CurrentUser.requireAnyRole(Roles.FINANCE);
        String status = normalize(paymentStatus);
        if ("PAID".equals(status)) {
            status = "SUCCESS";
        }
        if (status == null || !PAYMENT_STATUSES.contains(status)) {
            throw new BadRequestException("Payment status must be one of " + PAYMENT_STATUSES + ".");
        }
        Booking booking = findBooking(bookingId);
        if ("REFUNDED".equals(status) && !"SUCCESS".equals(booking.getPaymentStatus())) {
            throw new BadRequestException("Only successful payments can be refunded.");
        }

        booking.setPaymentStatus(status);
        if ("SUCCESS".equals(status) && PENDING.equals(booking.getBookingStatus())) {
            booking.setBookingStatus(CONFIRMED);
        }
        booking.setUpdatedAt(LocalDateTime.now());
        String finalStatus = status;
        paymentRepository.findByBooking_BookingId(bookingId).ifPresent(payment -> {
            payment.setStatus(finalStatus);
            paymentRepository.save(payment);
        });
        Booking updated = bookingRepository.save(booking);

        String message = "Payment for booking #" + updated.getBookingId() + " is now " + status.toLowerCase(Locale.ROOT) + ".";
        if (remarks != null && !remarks.isBlank()) {
            message += " " + remarks.trim();
        }
        notifyCustomer(updated, "PAYMENT_" + status, message);
        return toResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteBooking(Integer bookingId) {
        CurrentUser.requireAnyRole(); // ADMIN only
        Booking booking = findBooking(bookingId);
        if (paymentRepository.findByBooking_BookingId(bookingId).isPresent()
                || customerExperienceRepository.existsByBooking_BookingId(bookingId)) {
            throw new ConflictException("Booking #" + bookingId + " has payment or feedback records. Cancel it instead of deleting it.");
        }
        bookingRepository.delete(booking);
    }

    // ---- permission helpers ----

    private boolean isCustomer() {
        return CurrentUser.hasAnyRole(Roles.CUSTOMER);
    }

    private void requireOwnBooking(Booking booking) {
        if (!CurrentUser.isOwner(booking.getCustomer().getUserId())) {
            throw new AccessDeniedException("You can only manage your own bookings.");
        }
    }

    private void requireCanView(Booking booking) {
        if (CurrentUser.isAdmin() || CurrentUser.hasAnyRole(READ_ALL_ROLES)) {
            return;
        }
        if (isCustomer() && CurrentUser.isOwner(booking.getCustomer().getUserId())) {
            return;
        }
        SafariTrip trip = booking.getSafariTrip();
        if (CurrentUser.hasAnyRole(Roles.GUIDE) && trip != null && trip.getGuide() != null
                && CurrentUser.isOwner(trip.getGuide().getUserId())) {
            return;
        }
        throw new AccessDeniedException("You are not authorized to view this booking.");
    }

    // ---- booking rules ----

    private void requireBookableTrip(SafariTrip trip) {
        if (trip.getStatus() != null && UNBOOKABLE_TRIP_STATUSES.contains(trip.getStatus().toUpperCase(Locale.ROOT))) {
            throw new BadRequestException("This trip is " + trip.getStatus().toLowerCase(Locale.ROOT) + " and cannot be booked.");
        }
        if (!trip.getTripDate().atTime(trip.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BadRequestException("This trip has already started.");
        }
    }

    private void requireRequestMatchesTrip(BookingRequestDTO dto, SafariTrip trip) {
        if (dto.getPackageId() != null && !dto.getPackageId().equals(trip.getSafariPackage().getPackageId())) {
            throw new BadRequestException("The selected package does not match the trip.");
        }
        if (dto.getBoatId() != null && !dto.getBoatId().equals(trip.getBoat().getBoatId())) {
            throw new BadRequestException("The selected boat does not match the trip.");
        }
        if (dto.getTripDate() != null && !dto.getTripDate().equals(trip.getTripDate())) {
            throw new BadRequestException("The selected date does not match the trip.");
        }
    }

    private void requireBeforeCutoff(LocalDateTime tripStart) {
        if (LocalDateTime.now().isAfter(tripStart.minusHours(cutoffHours))) {
            throw new BadRequestException("Bookings can only be changed or cancelled up to " + cutoffHours
                    + " hours before the trip. Please contact customer relations.");
        }
    }

    /** Boat must be usable and the trip must have enough free seats (spec B.3). */
    private void validateCapacity(SafariTrip trip, Integer passengers, Integer excludeBookingId) {
        if (passengers == null || passengers < 1) {
            throw new BadRequestException("Number of passengers must be at least 1.");
        }
        Boat boat = trip.getBoat();
        if (!"OPERATIONAL".equalsIgnoreCase(boat.getStatus())) {
            throw new BadRequestException("The boat for this trip is not available (under maintenance or retired).");
        }
        if (boat.getCapacity() != null && passengers > boat.getCapacity()) {
            throw new BadRequestException("Passenger count exceeds boat capacity.");
        }
        SafariPackage safariPackage = trip.getSafariPackage();
        if (safariPackage.getMaxPassengers() != null && passengers > safariPackage.getMaxPassengers()) {
            throw new BadRequestException("Passenger count exceeds package capacity.");
        }
        int capacity = trip.getCapacity() != null ? trip.getCapacity() : boat.getCapacity();
        Long taken = bookingRepository.sumSeatsOnTrip(trip.getTripId(), SEAT_HOLDING, excludeBookingId);
        int available = capacity - (taken == null ? 0 : taken.intValue());
        if (passengers > available) {
            throw new BadRequestException(available <= 0
                    ? "This trip is fully booked."
                    : "Only " + available + " seat(s) left on this trip.");
        }
    }

    // ---- pricing (spec B.3: bookings keep the price and discount from the moment of booking) ----

    private void snapshotPrice(Booking booking, SafariPackage safariPackage) {
        booking.setUnitPrice(safariPackage.getBasePrice());
        booking.setDiscountPercentage(pricingService.bestDiscount(safariPackage, LocalDate.now()));
    }

    /** Bookings made before price snapshots existed: derive the unit price from the stored total. */
    private void keepLegacyPrice(Booking booking) {
        BigDecimal unit = booking.getTotalPrice() != null && booking.getNumberOfPeople() != null && booking.getNumberOfPeople() > 0
                ? booking.getTotalPrice().divide(BigDecimal.valueOf(booking.getNumberOfPeople()), 2, RoundingMode.HALF_UP)
                : booking.getSafariPackage().getBasePrice();
        booking.setUnitPrice(unit);
        booking.setDiscountPercentage(BigDecimal.ZERO);
    }

    private BigDecimal calculateTotal(Booking booking) {
        return pricingService.total(booking.getUnitPrice(), booking.getNumberOfPeople(), booking.getDiscountPercentage());
    }

    // ---- small helpers ----

    private void applyTrip(Booking booking, SafariTrip trip) {
        booking.setSafariTrip(trip);
        booking.setSafariPackage(trip.getSafariPackage());
        booking.setBoatId(trip.getBoat().getBoatId());
        booking.setTripDate(trip.getTripDate());
        booking.setTimeSlot(trip.getEndTime() == null ? trip.getStartTime().toString() : trip.getStartTime() + "-" + trip.getEndTime());
    }

    private LocalDateTime tripStart(Booking booking) {
        SafariTrip trip = booking.getSafariTrip();
        return trip != null ? trip.getTripDate().atTime(trip.getStartTime()) : booking.getTripDate().atStartOfDay();
    }

    private Booking findBooking(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
    }

    private SafariTrip findTrip(Integer tripId) {
        if (tripId == null) {
            throw new BadRequestException("A trip must be selected.");
        }
        return safariTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));
    }

    private void validatePaging(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page must be 0 or greater.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private void notifyCustomer(Booking booking, String type, String message) {
        notificationService.create(booking.getCustomer().getUserId(), type, message);
    }

    private BookingResponseDTO toResponseDTO(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setBookingId(booking.getBookingId());
        dto.setUserId(booking.getCustomer().getUserId());
        dto.setCustomerName(booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName());
        dto.setBoatId(booking.getBoatId());
        dto.setTripDate(booking.getTripDate());
        dto.setTimeSlot(booking.getTimeSlot());
        dto.setNumberOfPeople(booking.getNumberOfPeople());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setPaymentStatus(booking.getPaymentStatus());
        dto.setBookingStatus(booking.getBookingStatus());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        dto.setUnitPrice(booking.getUnitPrice());
        dto.setDiscountPercentage(booking.getDiscountPercentage());
        dto.setCreatedBy(booking.getCreatedBy());
        SafariTrip trip = booking.getSafariTrip();
        if (trip != null) {
            dto.setTripId(trip.getTripId());
            if (trip.getGuide() != null) {
                dto.setGuideName(trip.getGuide().getFirstName() + " " + trip.getGuide().getLastName());
            }
        }
        if (booking.getSafariPackage() != null) {
            dto.setPackageId(booking.getSafariPackage().getPackageId());
            dto.setPackageName(booking.getSafariPackage().getPackageName());
        }
        return dto;
    }
}
