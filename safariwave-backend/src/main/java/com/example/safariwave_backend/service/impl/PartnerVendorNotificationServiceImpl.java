package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.entity.PartnerVendorEntity;
import com.example.safariwave_backend.enums.PartnerVendorStatus;
import com.example.safariwave_backend.service.PartnerVendorNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PartnerVendorNotificationServiceImpl implements PartnerVendorNotificationService {
    private static final Logger log = LoggerFactory.getLogger(PartnerVendorNotificationServiceImpl.class);

    @Override
    public void notifyRegistration(PartnerVendorEntity partnerVendor) {
        log.info("Vendor registration event recorded for business {}", partnerVendor.getBusinessName());
    }

    @Override
    public void notifyStatusChange(PartnerVendorEntity partnerVendor, PartnerVendorStatus status) {
        log.info("Vendor status event recorded for business {} -> {}", partnerVendor.getBusinessName(), status);
    }
}
