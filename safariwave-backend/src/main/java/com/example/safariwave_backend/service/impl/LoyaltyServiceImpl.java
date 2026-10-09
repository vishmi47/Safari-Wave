package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.security.CurrentUser;
import com.example.safariwave_backend.security.Roles;
import com.example.safariwave_backend.dto.response.LoyaltyResponseDTO;
import com.example.safariwave_backend.entity.LoyaltyAccount;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.exception.BadRequestException;
import com.example.safariwave_backend.exception.ConflictException;
import com.example.safariwave_backend.exception.ResourceNotFoundException;
import com.example.safariwave_backend.repository.LoyaltyAccountRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.service.LoyaltyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class LoyaltyServiceImpl implements LoyaltyService {
    private final LoyaltyAccountRepository loyaltyRepository;
    private final UserRepository userRepository;
    public LoyaltyServiceImpl(LoyaltyAccountRepository loyaltyRepository, UserRepository userRepository) {
        this.loyaltyRepository = loyaltyRepository; this.userRepository = userRepository;
    }
    // Customer Experience module (spec 6.5): customers enrol and view their own account; customer relations manages all
    public LoyaltyResponseDTO enroll(Integer customerId) {
        CurrentUser.requireOwnerOrAnyRole(customerId, Roles.CUSTOMER_RELATIONS);
        if (loyaltyRepository.findByCustomer_UserId(customerId).isPresent()) throw new ConflictException("Customer is already enrolled in loyalty");
        User customer = userRepository.findById(customerId).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        LoyaltyAccount account = new LoyaltyAccount(); account.setCustomer(customer); return toDto(loyaltyRepository.save(account));
    }
    @Transactional(readOnly = true)
    public LoyaltyResponseDTO getByCustomer(Integer customerId) {
        CurrentUser.requireOwnerOrAnyRole(customerId, Roles.CUSTOMER_RELATIONS);
        return loyaltyRepository.findByCustomer_UserId(customerId).map(this::toDto).orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));
    }
    public LoyaltyResponseDTO updateStatus(Integer customerId, String status) {
        CurrentUser.requireAnyRole(Roles.CUSTOMER_RELATIONS);
        if (status == null || !java.util.List.of("ACTIVE", "SUSPENDED", "INACTIVE").contains(status.toUpperCase())) throw new BadRequestException("Invalid loyalty status");
        LoyaltyAccount account = loyaltyRepository.findByCustomer_UserId(customerId).orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));
        account.setStatus(status.toUpperCase()); return toDto(loyaltyRepository.save(account));
    }
    public LoyaltyResponseDTO addPoints(Integer customerId, long points) {
        CurrentUser.requireAnyRole(Roles.CUSTOMER_RELATIONS);
        if (points < 0) throw new BadRequestException("Points cannot be negative");
        LoyaltyAccount account = loyaltyRepository.findByCustomer_UserId(customerId).orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));
        account.setPoints(account.getPoints() + points); return toDto(loyaltyRepository.save(account));
    }
    private LoyaltyResponseDTO toDto(LoyaltyAccount account) { return new LoyaltyResponseDTO(account.getLoyaltyId(), account.getCustomer().getUserId(), account.getStatus(), account.getPoints(), account.getEnrolledAt(), account.getUpdatedAt()); }
}
