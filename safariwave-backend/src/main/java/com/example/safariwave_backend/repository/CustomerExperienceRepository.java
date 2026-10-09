package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.Booking;
import com.example.safariwave_backend.entity.CustomerExperience;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.enums.CustomerExperienceStatus;
import com.example.safariwave_backend.enums.CustomerExperienceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;

@Repository
public interface CustomerExperienceRepository extends JpaRepository<CustomerExperience, Integer> {

    List<CustomerExperience> findByCustomer(User customer);

    List<CustomerExperience> findByBooking(Booking booking);

    List<CustomerExperience> findByExperienceType(CustomerExperienceType experienceType);

    List<CustomerExperience> findByStatus(CustomerExperienceStatus status);

    List<CustomerExperience> findByExperienceTypeAndStatus(CustomerExperienceType experienceType, CustomerExperienceStatus status);

    List<CustomerExperience> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<CustomerExperience> findByExperienceTypeOrderByCreatedAtDesc(CustomerExperienceType experienceType);

    List<CustomerExperience> findAllByOrderByCreatedAtDesc();

    long countByStatus(CustomerExperienceStatus status);

    boolean existsByBooking_BookingId(Integer bookingId);

    // ---- Customer Experience module ----

    // Paged search, scoped by role: customerId (a customer's own), guideId (a guide's trips), ratedOnly (marketing)
    @Query("""
            SELECT e FROM CustomerExperience e JOIN e.customer c LEFT JOIN e.booking b LEFT JOIN b.safariTrip t LEFT JOIN t.guide g
            WHERE (:customerId IS NULL OR c.userId = :customerId)
              AND (:guideId IS NULL OR g.userId = :guideId)
              AND (:ratedOnly = false OR e.rating IS NOT NULL)
              AND e.experienceType IN :types
              AND (:status IS NULL OR e.status = :status)
              AND (:q IS NULL
                   OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(CONCAT(c.firstName, ' ', COALESCE(c.lastName, ''))) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<CustomerExperience> search(@Param("q") String q,
                                    @Param("status") CustomerExperienceStatus status,
                                    @Param("types") Collection<CustomerExperienceType> types,
                                    @Param("customerId") Integer customerId,
                                    @Param("guideId") Integer guideId,
                                    @Param("ratedOnly") boolean ratedOnly,
                                    Pageable pageable);

    List<CustomerExperience> findByRatingIsNotNull();

    // Feedback and ratings on trips guided by the given guide
    List<CustomerExperience> findByBooking_SafariTrip_Guide_UserIdAndExperienceTypeIn(Integer guideId, Collection<CustomerExperienceType> types);
}
