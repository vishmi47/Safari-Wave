package com.example.safariwave_backend.config;

import com.example.safariwave_backend.security.JwtAuthFilter;
import com.example.safariwave_backend.security.RestSecurityErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityFilterConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private RestSecurityErrorHandler restSecurityErrorHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 for missing/invalid tokens, 403 for roles that are not allowed (both as JSON)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restSecurityErrorHandler)
                        .accessDeniedHandler(restSecurityErrorHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        // Customer relations needs the customer list to book on a customer's behalf
                        .requestMatchers(HttpMethod.GET, "/api/users/customers").hasAnyRole("ADMIN", "GENERAL_MANAGER", "CUSTOMER_RELATIONS")
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "GENERAL_MANAGER")
                        // Promotion banners are shown in <img> tags, which cannot send a token
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        // /api/promotions/**: any logged-in user; per-role rules live in PromotionServiceImpl
                        // /api/packages/**: any logged-in user; per-role rules live in SafariPackageServiceImpl
                        // /api/partners-vendors/**: writes are ADMIN only; reads are filtered by role in PartnerVendorServiceImpl
                        // /api/weather-alerts, /api/safety-inspections, /api/emergency-incidents: per-role rules live in the safety services
                        .requestMatchers("/api/notifications/**").authenticated()
                        // /api/boats, /api/assets, /api/maintenance: any logged-in user; per-role rules live in the inventory services
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}