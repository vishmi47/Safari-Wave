package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.service.VendorService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;

    public VendorServiceImpl(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @Override
    public Vendor registerVendor(Vendor vendor) {
        if (vendorRepository.existsByEmail(vendor.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        vendor.setStatus(Vendor.VendorStatus.PENDING);
        vendor.setCreatedAt(LocalDateTime.now());
        return vendorRepository.save(vendor);
    }

    @Override
    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    @Override
    public List<Vendor> getPendingVendors() {
        return vendorRepository.findByStatus(Vendor.VendorStatus.PENDING);
    }

    @Override
    public Vendor approveVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        vendor.setStatus(Vendor.VendorStatus.APPROVED);
        vendor.setUpdatedAt(LocalDateTime.now());
        return vendorRepository.save(vendor);
    }

    @Override
    public Vendor rejectVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        vendor.setStatus(Vendor.VendorStatus.REJECTED);
        vendor.setUpdatedAt(LocalDateTime.now());
        return vendorRepository.save(vendor);
    }

    @Override
    public Vendor deactivateVendor(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
        vendor.setStatus(Vendor.VendorStatus.DEACTIVATED);
        vendor.setUpdatedAt(LocalDateTime.now());
        return vendorRepository.save(vendor);
    }
}
