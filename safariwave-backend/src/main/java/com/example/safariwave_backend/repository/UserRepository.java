package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    // Users whose role name is one of the given names (e.g. GUIDE and its alias SAFETY)
    List<User> findByRole_RoleNameInOrderByFirstNameAsc(Collection<String> roleNames);
}
