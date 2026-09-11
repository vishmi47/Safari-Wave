package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.SafariTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SafariTripRepository extends JpaRepository<SafariTrip, Integer> {
}