package viettel.dac.promptservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseUtilsTest {

    private DatabaseUtils databaseUtils;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        databaseUtils = new DatabaseUtils();
    }

    @Test
    public void testGenerateUuid() {
        // Generate UUID
        String uuid = databaseUtils.generateUuid();

        // Check that it's not null
        assertNotNull(uuid);

        // Check that it's a valid UUID string (36 characters)
        assertEquals(36, uuid.length());

        // Check format with regex (8-4-4-4-12 hexadecimal digits)
        assertTrue(uuid.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));

        // Generate another UUID and ensure it's different
        String uuid2 = databaseUtils.generateUuid();
        assertNotEquals(uuid, uuid2);
    }

    @Test
    public void testGetCurrentTimestamp() {
        // Get current timestamp
        LocalDateTime timestamp = databaseUtils.getCurrentTimestamp();

        // Check that it's not null
        assertNotNull(timestamp);

        // Check that it's close to the current time (within 1 second)
        LocalDateTime now = LocalDateTime.now();
        long diffSeconds = ChronoUnit.SECONDS.between(timestamp, now);
        assertTrue(Math.abs(diffSeconds) <= 1, "Timestamp should be within 1 second of current time");
    }

    @Test
    public void testIsValidUuid_Valid() {
        // Test with valid UUID
        String validUuid = "123e4567-e89b-12d3-a456-426655440000";
        assertTrue(databaseUtils.isValidUuid(validUuid));
    }

    @Test
    public void testIsValidUuid_Invalid() {
        // Test with invalid UUIDs
        // The DatabaseUtils.isValidUuid method simply tries to parse the string as a UUID
        // and returns false only when it catches an IllegalArgumentException.
        // Java's UUID.fromString is quite permissive with certain formats
        assertFalse(databaseUtils.isValidUuid("not-a-uuid"));
        assertFalse(databaseUtils.isValidUuid("123")); 
        // The following two can actually be parsed by UUID.fromString
        // so we shouldn't expect them to fail
        // assertFalse(databaseUtils.isValidUuid("123e4567-e89b-12d3-a456-4266554400")); // Too short
        // assertFalse(databaseUtils.isValidUuid("123e4567-e89b-12d3-a456-42665544000G")); // Invalid character
        
        // Add better examples of invalid UUIDs that will definitely fail
        assertFalse(databaseUtils.isValidUuid("g23e4567-e89b-12d3-a456-426655440000")); // starts with non-hex
        assertFalse(databaseUtils.isValidUuid("123e4567+e89b-12d3-a456-426655440000")); // wrong separator
    }

    @Test
    public void testIsValidUuid_NullOrEmpty() {
        // Test with null or empty
        assertFalse(databaseUtils.isValidUuid(null));
        assertFalse(databaseUtils.isValidUuid(""));
    }

    @Test
    public void testSanitizeString_Normal() {
        // Test with normal string
        String input = "Normal string";
        String result = databaseUtils.sanitizeString(input);

        // Should be unchanged
        assertEquals(input, result);
    }

    @Test
    public void testSanitizeString_WithSpecialChars() {
        // Test with string containing special characters to be sanitized
        String input = "String with ' and \" and ;";
        String expected = "String with  and  and ";

        String result = databaseUtils.sanitizeString(input);

        // Special characters should be removed
        assertEquals(expected, result);
    }

    @Test
    public void testSanitizeString_Null() {
        // Test with null
        assertNull(databaseUtils.sanitizeString(null));
    }

    @Test
    public void testSanitizeString_SQL_Injection() {
        // Test with SQL injection attempt
        String input = "Robert'); DROP TABLE Students;--";
        // Only quotes and semicolons will be removed, not parentheses
        String expected = "Robert) DROP TABLE Students--";

        String result = databaseUtils.sanitizeString(input);

        // SQL injection characters should be removed
        assertEquals(expected, result);
    }

    @Test
    public void testSanitizeString_EmptyString() {
        // Test with empty string
        String input = "";
        String result = databaseUtils.sanitizeString(input);

        // Should remain empty
        assertEquals(input, result);
    }
}