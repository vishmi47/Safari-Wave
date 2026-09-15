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
}
