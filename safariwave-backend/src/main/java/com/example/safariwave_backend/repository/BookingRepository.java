package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByCustomer_UserId(Integer userId);

    List<Booking> findByBookingStatus(String bookingStatus);

    // Used to check if a boat is already booked for a given date + time slot
    // before confirming a new booking (avoids double-booking).
    List<Booking> findByBoatIdAndTripDateAndTimeSlot(Integer boatId, LocalDate tripDate, String timeSlot);

    boolean existsByBoatIdAndTripDateAndTimeSlotAndBookingStatusIn(
            Integer boatId, LocalDate tripDate, String timeSlot, List<String> bookingStatuses);

    // Used by the package module to decide between hard delete and archiving
    boolean existsBySafariPackage_PackageId(Integer packageId);

    // ---- Booking & Reservation module ----

    // Seats already taken on a trip by bookings in the given statuses; excludeBookingId skips the booking being edited
    @Query("""
            SELECT COALESCE(SUM(b.numberOfPeople), 0) FROM Booking b
            WHERE b.safariTrip.tripId = :tripId
              AND b.bookingStatus IN :statuses
              AND (:excludeBookingId IS NULL OR b.bookingId <> :excludeBookingId)
            """)
    Long sumSeatsOnTrip(@Param("tripId") Integer tripId,
                        @Param("statuses") List<String> statuses,
                        @Param("excludeBookingId") Integer excludeBookingId);

    List<Booking> findBySafariTrip_TripIdAndBookingStatusIn(Integer tripId, List<String> statuses);

    boolean existsBySafariTrip_TripId(Integer tripId);

    // Bookings on trips assigned to a guide
    List<Booking> findBySafariTrip_Guide_UserId(Integer guideId);

    // Paged search; customerId / guideId limit results to one customer's bookings or one guide's trips
    @Query("""
            SELECT b FROM Booking b JOIN b.customer c LEFT JOIN b.safariTrip t LEFT JOIN t.guide g
            WHERE (:customerId IS NULL OR c.userId = :customerId)
              AND (:guideId IS NULL OR g.userId = :guideId)
              AND (:status IS NULL OR b.bookingStatus = :status)
              AND (:q IS NULL
                   OR LOWER(CONCAT(c.firstName, ' ', COALESCE(c.lastName, ''))) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR CAST(b.bookingId AS string) = :q)
            """)
    Page<Booking> search(@Param("q") String q,
                         @Param("status") String status,
                         @Param("customerId") Integer customerId,
                         @Param("guideId") Integer guideId,
                         Pageable pageable);

    // A customer's bookings in the given statuses (e.g. upcoming ones, for weather alerts)
    List<Booking> findByCustomer_UserIdAndBookingStatusIn(Integer userId, List<String> statuses);
}
