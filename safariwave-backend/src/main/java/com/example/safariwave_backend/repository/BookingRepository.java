package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByCustomer_UserId(Integer userId);

    List<Booking> findByBookingStatus(String bookingStatus);

    // Used to check if a boat is already booked for a given date + time slot
    // before confirming a new booking (avoids double-booking).
    List<Booking> findByBoatIdAndTripDateAndTimeSlot(Integer boatId, LocalDate tripDate, String timeSlot);
}
