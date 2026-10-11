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
import com.example.safariwave_backend.service.payment.PaymentContext;
import com.example.safariwave_backend.service.payment.PaymentOutcome;
import com.example.safariwave_backend.service.payment.PaymentStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final PaymentContext paymentContext;
    public PaymentServiceImpl(PaymentRepository paymentRepository, BookingRepository bookingRepository, NotificationService notificationService, PaymentContext paymentContext) {
        this.paymentRepository = paymentRepository; this.bookingRepository = bookingRepository; this.notificationService = notificationService; this.paymentContext = paymentContext;
    }
    public PaymentResponseDTO processMockPayment(Integer bookingId, PaymentRequestDTO request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        // The booking's customer pays; customer relations and finance may record a payment for them
        CurrentUser.requireOwnerOrAnyRole(booking.getCustomer().getUserId(), Roles.CUSTOMER_RELATIONS, Roles.FINANCE);
        if (!"PENDING".equals(booking.getBookingStatus())) throw new BadRequestException("Only pending bookings can be paid");
        if (paymentRepository.findByBooking_BookingId(bookingId).isPresent()) throw new BadRequestException("A payment already exists for this booking");
        // Strategy pattern: the requested method (CARD / CASH) decides how the payment is taken
        PaymentStrategy strategy = paymentContext.strategyFor(request.getMethod());
        PaymentOutcome outcome = strategy.pay(booking, request);
        Payment payment = new Payment(); payment.setBooking(booking); payment.setAmount(booking.getTotalPrice()); payment.setStatus(outcome.status()); payment.setProvider(strategy.provider()); payment.setFailureReason(outcome.failureReason());
        booking.setPaymentStatus(outcome.status()); booking.setBookingStatus(outcome.succeeded() ? "CONFIRMED" : "PENDING");
        Booking savedBooking = bookingRepository.save(booking);
        notificationService.create(savedBooking.getCustomer().getUserId(), "PAYMENT_" + outcome.status(), "Payment " + outcome.status().toLowerCase() + " for booking " + bookingId + " (" + strategy.provider() + ")");
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
