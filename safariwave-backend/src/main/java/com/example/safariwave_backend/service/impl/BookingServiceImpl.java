package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.BookingRequestDTO;
import com.example.safariwave_backend.dto.response.BookingResponseDTO;
import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.repository.BookingRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        User customer = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + dto.getUserId()));

        // Reject the booking if the same boat is already booked for the same date + time slot.
        List<Booking> clashes = bookingRepository.findByBoatIdAndTripDateAndTimeSlot(
                dto.getBoatId(), dto.getTripDate(), dto.getTimeSlot());
        if (!clashes.isEmpty()) {
            throw new RuntimeException("Boat is already booked for the selected date and time slot");
        }

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setBoatId(dto.getBoatId());
        booking.setTripDate(dto.getTripDate());
        booking.setTimeSlot(dto.getTimeSlot());
        booking.setNumberOfPeople(dto.getNumberOfPeople());
        booking.setTotalPrice(dto.getTotalPrice());
        booking.setPaymentStatus(dto.getPaymentStatus());
        booking.setBookingStatus(dto.getBookingStatus());
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<BookingResponseDTO> getBookingById(Integer bookingId) {
        return bookingRepository.findById(bookingId).map(this::toResponseDTO);
    }

    @Override
    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDTO> getBookingsByUserId(Integer userId) {
        return bookingRepository.findByCustomer_UserId(userId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponseDTO> getBookingsByStatus(String bookingStatus) {
        return bookingRepository.findByBookingStatus(bookingStatus)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BookingResponseDTO updateBooking(Integer bookingId, BookingRequestDTO dto) {
        Booking existingBooking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        existingBooking.setBoatId(dto.getBoatId());
        existingBooking.setTripDate(dto.getTripDate());
        existingBooking.setTimeSlot(dto.getTimeSlot());
        existingBooking.setNumberOfPeople(dto.getNumberOfPeople());
        existingBooking.setTotalPrice(dto.getTotalPrice());
        existingBooking.setPaymentStatus(dto.getPaymentStatus());
        existingBooking.setBookingStatus(dto.getBookingStatus());
        existingBooking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(existingBooking);
        return toResponseDTO(updated);
    }

    @Override
    public BookingResponseDTO updateBookingStatus(Integer bookingId, String bookingStatus) {
        Booking existingBooking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        existingBooking.setBookingStatus(bookingStatus);
        existingBooking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(existingBooking);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteBooking(Integer bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new RuntimeException("Booking not found with id: " + bookingId);
        }
        bookingRepository.deleteById(bookingId);
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
        return dto;
    }
}
