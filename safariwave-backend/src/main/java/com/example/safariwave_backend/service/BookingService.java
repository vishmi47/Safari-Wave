package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.BookingRequestDTO;
import com.example.safariwave_backend.dto.response.BookingResponseDTO;

import java.util.List;
import java.util.Optional;

public interface BookingService {

    BookingResponseDTO createBooking(BookingRequestDTO dto);

    Optional<BookingResponseDTO> getBookingById(Integer bookingId);

    List<BookingResponseDTO> getAllBookings();

    List<BookingResponseDTO> getBookingsByUserId(Integer userId);

    List<BookingResponseDTO> getBookingsByStatus(String bookingStatus);

    BookingResponseDTO updateBooking(Integer bookingId, BookingRequestDTO dto);

    BookingResponseDTO updateBookingStatus(Integer bookingId, String bookingStatus);

    void deleteBooking(Integer bookingId);
}
