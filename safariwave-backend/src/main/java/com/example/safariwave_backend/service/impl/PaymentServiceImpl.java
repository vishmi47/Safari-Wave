package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.PaymentRequestDTO;
import com.example.safariwave_backend.dto.response.PaymentResponseDTO;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.Payment;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.PaymentRepository;
import com.example.safariwave_backend.service.NotificationService;
import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository, NotificationService notificationService) {
        this.paymentRepository = paymentRepository; this.bookingRepository = bookingRepository; this.notificationService = notificationService;
    }
    public PaymentResponseDTO processMockPayment(Integer bookingId, PaymentRequestDTO request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        // The booking's customer pays; customer relations and finance may record a payment for them
        CurrentUser.requireOwnerOrAnyRole(booking.getCustomer().getUserId(), Roles.CUSTOMER_RELATIONS, Roles.FINANCE);
        if (!"PENDING".equals(booking.getBookingStatus())) throw new BadRequestException("Only pending bookings can be paid");
        if (paymentRepository.findByBooking_BookingId(bookingId).isPresent()) throw new BadRequestException("A payment already exists for this booking");
        String outcome = request.getOutcome().trim().toUpperCase();
        if (!java.util.List.of("SUCCESS", "FAILED").contains(outcome)) throw new BadRequestException("Mock outcome must be SUCCESS or FAILED");
        Payment payment = new Payment(); payment.setBooking(booking); payment.setAmount(booking.getTotalPrice()); payment.setStatus(outcome); payment.setFailureReason("FAILED".equals(outcome) ? "Mock payment declined" : null);
        booking.setPaymentStatus(outcome); booking.setBookingStatus("SUCCESS".equals(outcome) ? "CONFIRMED" : "PENDING");
        Booking savedBooking = bookingRepository.save(booking);
        notificationService.create(savedBooking.getCustomer().getUserId(), "PAYMENT_" + outcome, "Mock payment " + outcome.toLowerCase() + " for booking " + bookingId);
        return toDto(paymentRepository.save(payment));
    }
    @Transactional(readOnly = true)
    public PaymentResponseDTO getByBooking(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        CurrentUser.requireOwnerOrAnyRole(booking.getCustomer().getUserId(), Roles.CUSTOMER_RELATIONS, Roles.FINANCE, Roles.OPERATIONS);
        return paymentRepository.findByBooking_BookingId(bookingId).map(this::toDto).orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }
    private PaymentResponseDTO toDto(Payment p) { return new PaymentResponseDTO(p.getPaymentId(), p.getBooking().getBookingId(), p.getAmount(), p.getStatus(), p.getProvider(), p.getFailureReason(), p.getCreatedAt()); }
}
