package com.example.safariwave_backend.service;

import com.example.safariwave_backend.entity.Vendor;
import java.util.List;

/**
 * Legacy duplicate kept so existing teammate code keeps compiling.
 * @deprecated use PartnerVendorService instead (canonical class for the Partner & Vendor module).
 */
@Deprecated
public interface VendorService {

    Vendor registerVendor(Vendor vendor);

    List<Vendor> getAllVendors();

    List<Vendor> getPendingVendors();

    Vendor approveVendor(Long id);

    Vendor rejectVendor(Long id);

    Vendor deactivateVendor(Long id);
}