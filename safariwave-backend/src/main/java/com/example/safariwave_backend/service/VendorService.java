package com.example.safariwave_backend.service;

import com.example.safariwave_backend.entity.Vendor;
import java.util.List;

/**
 * FR-06: Partner & Vendor Management Service Interface
 * Matches the AuthService interface pattern for consistency
 */
public interface VendorService {

    /**
     * Register a new partner/vendor with PENDING status
     */
    Vendor registerVendor(Vendor vendor);

    /**
     * Get all vendors (Admin/GM view)
     */
    List<Vendor> getAllVendors();

    /**
     * Get only pending vendor approvals
     */
    List<Vendor> getPendingVendors();

    /**
     * Approve a pending vendor (PENDING → APPROVED)
     */
    Vendor approveVendor(Long id);

    /**
     * Reject a pending vendor (PENDING → REJECTED)
     */
    Vendor rejectVendor(Long id);

    /**
     * Deactivate an approved vendor (APPROVED → DEACTIVATED)
     */
    Vendor deactivateVendor(Long id);
}