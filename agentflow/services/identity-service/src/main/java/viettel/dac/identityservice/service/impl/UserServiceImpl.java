package viettel.dac.identityservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.common.exception.AuthenticationException;
import viettel.dac.identityservice.common.exception.ResourceAlreadyExistsException;
import viettel.dac.identityservice.common.exception.ResourceNotFoundException;
import viettel.dac.identityservice.dto.mapper.UserMapper;
import viettel.dac.identityservice.dto.request.UserRegistrationRequest;
import viettel.dac.identityservice.dto.request.UserUpdateRequest;
import viettel.dac.identityservice.dto.response.UserResponse;
import viettel.dac.identityservice.entity.User;
import viettel.dac.identityservice.entity.VerificationToken;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.repository.VerificationTokenRepository;
import viettel.dac.identityservice.service.AuditService;
import viettel.dac.identityservice.service.UserService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;

    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already registered");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username already taken");
        }

        // Create new user
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(User.UserStatus.PENDING) // Start as pending until email verification
                .emailVerified(false)
                .mfaEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Create email verification token
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenValue)
                .userId(savedUser.getId())
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusDays(1)) // 24 hour expiry
                .used(false)
                .build();

        verificationTokenRepository.save(verificationToken);

        // TODO: Send verification email with token
        log.info("Email verification token created for user {}: {}", savedUser.getId(), tokenValue);

        // Log user registration
        auditService.logResourceEvent("user", savedUser.getId(), "create",
                Map.of("email", savedUser.getEmail(), "username", savedUser.getUsername()));

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(String id) {
        return userRepository.findById(id)
                .map(userMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toUserResponse);
    }

    @Override
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if email is being changed and is already in use
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ResourceAlreadyExistsException("Email already registered");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false); // Require verification of new email

            // Create email verification token
            String tokenValue = UUID.randomUUID().toString();
            VerificationToken verificationToken = VerificationToken.builder()
                    .token(tokenValue)
                    .userId(user.getId())
                    .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .used(false)
                    .build();

            verificationTokenRepository.save(verificationToken);

            // TODO: Send verification email with token
            log.info("Email verification token created for user {}: {}", user.getId(), tokenValue);
        }

        // Check if username is being changed and is already in use
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ResourceAlreadyExistsException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }

        // Update other fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);

        // Log user update
        auditService.logResourceEvent("user", updatedUser.getId(), "update",
                Map.of("email", updatedUser.getEmail(), "username", updatedUser.getUsername()));

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    public boolean deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Instead of actually deleting, mark as inactive
        user.setStatus(User.UserStatus.INACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Log user deactivation
        auditService.logResourceEvent("user", user.getId(), "delete",
                Map.of("email", user.getEmail(), "username", user.getUsername()));

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String searchTerm, User.UserStatus status, Pageable pageable) {
        Page<User> users;

        if (searchTerm != null && status != null) {
            // Search by term and status
            users = userRepository.findBySearchTermAndStatus(searchTerm, status, pageable);
        } else if (searchTerm != null) {
            // Search by term only
            users = userRepository.findBySearchTerm(searchTerm, pageable);
        } else if (status != null) {
            // Filter by status only
            users = userRepository.findByStatus(status, pageable);
        } else {
            // No filters, get all users
            users = userRepository.findAll(pageable);
        }

        return users.map(userMapper::toUserResponse);
    }

    @Override
    public UserResponse enableMfa(String userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (enable && !user.isMfaEnabled()) {
            // TODO: Generate MFA secret and return setup information
            user.setMfaEnabled(true);
            user.setMfaSecret("DUMMY_SECRET"); // This would be a proper TOTP secret in production
        } else if (!enable && user.isMfaEnabled()) {
            user.setMfaEnabled(false);
            user.setMfaSecret(null);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        // Log MFA status change
        auditService.logResourceEvent("user", user.getId(), "mfa",
                Map.of("action", enable ? "enabled" : "disabled"));

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    public boolean lockUser(String userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setStatus(User.UserStatus.LOCKED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Log user lock
        auditService.logResourceEvent("user", user.getId(), "lock",
                Map.of("reason", reason != null ? reason : "administrative action"));

        return true;
    }

    @Override
    public boolean unlockUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == User.UserStatus.LOCKED) {
            user.setStatus(User.UserStatus.ACTIVE);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Log user unlock
            auditService.logResourceEvent("user", user.getId(), "unlock", Collections.emptyMap());
        }

        return true;
    }

    @Override
    public boolean changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Log password change
        auditService.logResourceEvent("user", user.getId(), "password_change", Collections.emptyMap());

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}