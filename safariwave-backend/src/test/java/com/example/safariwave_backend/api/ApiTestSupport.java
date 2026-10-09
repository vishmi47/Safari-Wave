package com.example.safariwave_backend.api;

import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.Role;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.entity.User;
import com.example.safariwave_backend.repository.BoatRepository;
import com.example.safariwave_backend.repository.RoleRepository;
import com.example.safariwave_backend.repository.SafariPackageRepository;
import com.example.safariwave_backend.repository.UserRepository;
import com.example.safariwave_backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for API tests: boots the whole app on the in-memory H2 test database and calls endpoints through
 * MockMvc with real JWTs, so security rules, controllers and JSON are tested together. Each test rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiTestSupport {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected BoatRepository boatRepository;

    @Autowired
    protected SafariPackageRepository safariPackageRepository;

    @Autowired
    protected JwtUtil jwtUtil;

    /** A logged-in test user: the saved entity plus a bearer token. */
    protected record TestUser(User user, String token) {
        public Integer id() {
            return user.getUserId();
        }
    }

    protected TestUser createUser(String roleName) {
        Role role = roleRepository.findFirstByRoleNameIgnoreCase(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not seeded: " + roleName));
        int n = SEQUENCE.incrementAndGet();
        User user = new User();
        user.setRole(role);
        user.setFirstName(roleName.charAt(0) + roleName.substring(1).toLowerCase() + n);
        user.setLastName("Tester");
        user.setEmail(roleName.toLowerCase() + n + "@safariwave.test");
        user.setPassword("not-used-in-api-tests");
        user.setStatus("ACTIVE");
        User saved = userRepository.save(user);
        return new TestUser(saved, jwtUtil.generateToken(saved.getEmail(), saved.getUserId(), role.getRoleName()));
    }

    protected Boat createBoat(int capacity, String status) {
        Boat boat = new Boat();
        boat.setBoatName("Heron " + SEQUENCE.incrementAndGet());
        boat.setRegistrationNumber("REG-" + SEQUENCE.incrementAndGet());
        boat.setCapacity(capacity);
        boat.setStatus(status);
        return boatRepository.save(boat);
    }

    protected SafariPackage createPackage(String price, int maxPassengers) {
        SafariPackage safariPackage = new SafariPackage();
        safariPackage.setPackageName("Lagoon Tour " + SEQUENCE.incrementAndGet());
        safariPackage.setDuration(2);
        safariPackage.setBasePrice(new BigDecimal(price));
        safariPackage.setMaxPassengers(maxPassengers);
        safariPackage.setStatus("ACTIVE");
        return safariPackageRepository.save(safariPackage);
    }

    /** Adds the bearer token and JSON content type to a request. */
    protected MockHttpServletRequestBuilder as(TestUser user, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + user.token()).contentType(MediaType.APPLICATION_JSON);
    }
}
