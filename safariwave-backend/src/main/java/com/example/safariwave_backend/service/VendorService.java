package com.example.safariwave_backend.service;

import java.util.List;

public interface VendorService {

    Vendor registerVendor(Vendor vendor);

    List<Vendor> getAllVendors();

    List<Vendor> getPendingVendors();

    Vendor approveVendor(Long id);

    Vendor rejectVendor(Long id);

    Vendor deactivateVendor(Long id);
}