package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.entity.PartnerVendorEntity;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import com.example.safariwave_backend.service.PartnerVendorNotificationService;
import org.springframework.stereotype.Service;

@Service
public class PartnerVendorNotificationServiceImpl implements PartnerVendorNotificationService {

    @Override
    public void notifyRegistration(PartnerVendorEntity partnerVendor) {
        System.out.println("Vendor registration notification sent for: " + partnerVendor.getBusinessName()
                + " with status " + partnerVendor.getStatus());
    }

    @Override
    public void notifyStatusChange(PartnerVendorEntity partnerVendor, PartnerVendorStatus status) {
        System.out.println("Vendor status update notification sent for: " + partnerVendor.getBusinessName()
                + " changed to " + status);
    }
}
