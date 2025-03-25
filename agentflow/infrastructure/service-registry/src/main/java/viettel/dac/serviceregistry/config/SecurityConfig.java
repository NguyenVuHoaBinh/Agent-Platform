package viettel.dac.serviceregistry.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Eureka Server
 * Handles authentication for Eureka dashboard and Actuator endpoints
 */
@Configuration
public class SecurityConfig {

    @Value("${spring.security.user.name:admin}")
    private String username;

    @Value("${spring.security.user.password:admin}")
    private String password;

    @Value("${management.security.roles:ACTUATOR}")
    private String actuatorRole;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails adminUser = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("ADMIN", actuatorRole)
                .build();

        // Create a specific user for monitoring systems with limited access
        UserDetails monitorUser = User.builder()
                .username("monitor")
                .password(passwordEncoder().encode("monitor"))
                .roles(actuatorRole)
                .build();

        return new InMemoryUserDetailsManager(adminUser, monitorUser);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints for basic health monitoring and kubernetes probes
                        .requestMatchers("/actuator/health", "/actuator/health/liveness",
                                "/actuator/health/readiness", "/actuator/info")
                        .permitAll()
                        // Secured endpoints requiring ACTUATOR role
                        .requestMatchers("/actuator/metrics/**", "/actuator/prometheus",
                                "/actuator/loggers/**", "/actuator/env/**")
                        .hasRole(actuatorRole)
                        // Admin-only endpoints
                        .requestMatchers("/actuator/httptrace/**")
                        .hasRole("ADMIN")
                )
                .httpBasic(basic -> {})
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {})
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}