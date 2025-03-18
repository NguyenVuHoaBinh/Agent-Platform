package viettel.dac.identityservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import viettel.dac.identityservice.model.User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_withExistingUsername_shouldReturnUser() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build();
        userRepository.save(user);

        // Act
        Optional<User> result = userRepository.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void findByUsername_withNonExistingUsername_shouldReturnEmpty() {
        // Act
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void findByEmail_withExistingEmail_shouldReturnUser() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build();
        userRepository.save(user);

        // Act
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void existsByUsername_withExistingUsername_shouldReturnTrue() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build();
        userRepository.save(user);

        // Act
        boolean result = userRepository.existsByUsername("testuser");

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByUsername_withNonExistingUsername_shouldReturnFalse() {
        // Act
        boolean result = userRepository.existsByUsername("nonexistent");

        // Assert
        assertFalse(result);
    }

    @Test
    void existsByEmail_withExistingEmail_shouldReturnTrue() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build();
        userRepository.save(user);

        // Act
        boolean result = userRepository.existsByEmail("test@example.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByEmail_withNonExistingEmail_shouldReturnFalse() {
        // Act
        boolean result = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result);
    }
}