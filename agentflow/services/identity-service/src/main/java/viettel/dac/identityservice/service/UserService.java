package viettel.dac.identityservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import viettel.dac.identityservice.dto.request.UserRegistrationRequest;
import viettel.dac.identityservice.dto.request.UserUpdateRequest;
import viettel.dac.identityservice.dto.response.UserResponse;
import viettel.dac.identityservice.entity.User;

import java.util.Optional;

/**
 * Service for user management operations
 */
public interface UserService {

    /**
     * Register a new user
     *
     * @param request User registration data
     * @return Created user information
     */
    UserResponse registerUser(UserRegistrationRequest request);

    /**
     * Get user by ID
     *
     * @param id User ID
     * @return User information if found
     */
    Optional<UserResponse> getUserById(String id);

    /**
     * Get user by email
     *
     * @param email User email
     * @return User information if found
     */
    Optional<UserResponse> getUserByEmail(String email);

    /**
     * Get user by username
     *
     * @param username Username
     * @return User information if found
     */
    Optional<UserResponse> getUserByUsername(String username);

    /**
     * Update user information
     *
     * @param id User ID
     * @param request User update data
     * @return Updated user information
     */
    UserResponse updateUser(String id, UserUpdateRequest request);

    /**
     * Delete a user
     *
     * @param id User ID
     * @return true if successfully deleted
     */
    boolean deleteUser(String id);

    /**
     * Search users by criteria
     *
     * @param searchTerm Search term for name, email, username
     * @param status User status filter
     * @param pageable Pagination information
     * @return Page of matching users
     */
    Page<UserResponse> searchUsers(String searchTerm, User.UserStatus status, Pageable pageable);

    /**
     * Enable or disable MFA for a user
     *
     * @param userId User ID
     * @param enable True to enable, false to disable
     * @return Updated user information
     */
    UserResponse enableMfa(String userId, boolean enable);

    /**
     * Lock a user account
     *
     * @param userId User ID
     * @param reason Reason for locking
     * @return true if successfully locked
     */
    boolean lockUser(String userId, String reason);

    /**
     * Unlock a user account
     *
     * @param userId User ID
     * @return true if successfully unlocked
     */
    boolean unlockUser(String userId);

    /**
     * Change user password
     *
     * @param userId User ID
     * @param currentPassword Current password for verification
     * @param newPassword New password
     * @return true if password changed successfully
     */
    boolean changePassword(String userId, String currentPassword, String newPassword);

    /**
     * Check if email exists
     *
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists
     *
     * @param username Username to check
     * @return true if username exists
     */
    boolean existsByUsername(String username);
}