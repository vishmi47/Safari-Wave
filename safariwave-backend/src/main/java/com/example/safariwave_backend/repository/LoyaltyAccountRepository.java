package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Integer> {
    Optional<LoyaltyAccount> findByCustomer_UserId(Integer customerId);
}
