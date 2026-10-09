package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.BookingRequestDTO;
import com.example.safariwave_backend.dto.request.StatusUpdateRequestDTO;
import com.example.safariwave_backend.dto.response.BookingResponseDTO;
import com.example.safariwave_backend.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

// Role and ownership rules for bookings are enforced in BookingServiceImpl.
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(@Valid @RequestBody BookingRequestDTO dto) {
        BookingResponseDTO saved = bookingService.createBooking(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    // Paged list with search: GET /api/bookings/search?q=nimal&status=PENDING&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<PagedModel<BookingResponseDTO>> searchBookings(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(bookingService.searchBookings(q, status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Integer id) {
        return bookingService.getBookingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(bookingService.getBookingsByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> updateBooking(@PathVariable Integer id, @Valid @RequestBody BookingRequestDTO dto) {
        BookingResponseDTO updated = bookingService.updateBooking(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingResponseDTO> updateBookingStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String bookingStatus = body.get("bookingStatus");
        BookingResponseDTO updated = bookingService.updateBookingStatus(id, bookingStatus);
        return ResponseEntity.ok(updated);
    }

    // Customers (own, before the cutoff) and customer relations cancel; the booking is kept with status CANCELLED
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(@PathVariable Integer id,
                                                            @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(bookingService.cancelBooking(id, reason));
    }

    // Finance: payment / refund status only
    @PatchMapping("/{id}/payment-status")
    public ResponseEntity<BookingResponseDTO> updatePaymentStatus(@PathVariable Integer id,
                                                                  @Valid @RequestBody StatusUpdateRequestDTO dto) {
        return ResponseEntity.ok(bookingService.updatePaymentStatus(id, dto.getStatus(), dto.getRemarks()));
    }

    // Hard delete: ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Integer id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
