package com.example.safariwave_backend.service;

import com.example.safariwave_backend.entity.PartnerVendorEntity;
import com.example.safariwave_backend.enums.PartnerVendorStatus;

public interface PartnerVendorNotificationService {

    void notifyRegistration(PartnerVendorEntity partnerVendor);

    void notifyStatusChange(PartnerVendorEntity partnerVendor, PartnerVendorStatus status);
}