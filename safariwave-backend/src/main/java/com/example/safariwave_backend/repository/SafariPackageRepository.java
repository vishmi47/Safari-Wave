package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.SafariPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SafariPackageRepository extends JpaRepository<SafariPackage, Integer> {
}