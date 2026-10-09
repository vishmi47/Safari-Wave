package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.BookingRequestDTO;
import com.example.safariwave_backend.dto.response.BookingResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface BookingService {

    BookingResponseDTO createBooking(BookingRequestDTO dto);

    Optional<BookingResponseDTO> getBookingById(Integer bookingId);

    List<BookingResponseDTO> getAllBookings();

    List<BookingResponseDTO> getBookingsByUserId(Integer userId);

    List<BookingResponseDTO> getBookingsByStatus(String bookingStatus);

    /** Paged search; customers see their own bookings and guides see bookings on their assigned trips. */
    Page<BookingResponseDTO> searchBookings(String query, String status, int page, int size);

    BookingResponseDTO updateBooking(Integer bookingId, BookingRequestDTO dto);

    BookingResponseDTO updateBookingStatus(Integer bookingId, String bookingStatus);

    /** Sets status CANCELLED (spec B.3: cancellation is not a delete). */
    BookingResponseDTO cancelBooking(Integer bookingId, String reason);

    /** Finance updates payment / refund status only. */
    BookingResponseDTO updatePaymentStatus(Integer bookingId, String paymentStatus, String remarks);

    /** Hard delete, ADMIN only. */
    void deleteBooking(Integer bookingId);
}
