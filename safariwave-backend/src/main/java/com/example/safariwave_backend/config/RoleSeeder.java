package com.example.safariwave_backend.config;

import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.repository.RoleRepository;
import com.example.safariwave_backend.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Makes sure every role in {@link Roles#ALL} exists at startup. Existing rows are never changed or renamed,
 * and a role counts as present if its alias is already stored (e.g. GENERAL_MANAGER satisfies ADMIN).
 */
@Component
public class RoleSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> existing = roleRepository.findAll().stream()
                .map(Role::getRoleName)
                .filter(name -> name != null)
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        for (String roleName : Roles.ALL) {
            boolean present = existing.contains(roleName)
                    || Roles.aliasesOf(roleName).stream().anyMatch(existing::contains);
            if (!present) {
                Role role = new Role();
                role.setRoleName(roleName);
                roleRepository.save(role);
                log.info("Seeded missing role {}", roleName);
            }
        }
    }
}
