package viettel.dac.promptservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests to verify that Flyway migrations are correctly applied.
 * This tests against an in-memory H2 database.
 */
@SpringBootTest
@ActiveProfiles("test")
public class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testMigrationApplied() {
        // Verify that all tables were created
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                String.class
        );

        // Check for our main tables
        assertTrue(tables.contains("PROMPT_TEMPLATES"));
        assertTrue(tables.contains("PROMPT_VERSIONS"));
        assertTrue(tables.contains("PROMPT_PARAMETERS"));
        assertTrue(tables.contains("PROMPT_EXECUTIONS"));

        // Check for reference data tables
        assertTrue(tables.contains("PARAMETER_TYPES"));
        assertTrue(tables.contains("VERSION_STATUSES"));
        assertTrue(tables.contains("EXECUTION_STATUSES"));
    }

    @Test
    public void testReferenceDataLoaded() {
        // Check that reference data was correctly inserted
        List<Map<String, Object>> paramTypes = jdbcTemplate.queryForList(
                "SELECT COUNT(*) as count FROM PARAMETER_TYPES"
        );
        assertEquals(7, ((Number) paramTypes.get(0).get("count")).intValue());

        List<Map<String, Object>> versionStatuses = jdbcTemplate.queryForList(
                "SELECT COUNT(*) as count FROM VERSION_STATUSES"
        );
        assertEquals(6, ((Number) versionStatuses.get(0).get("count")).intValue());

        List<Map<String, Object>> executionStatuses = jdbcTemplate.queryForList(
                "SELECT COUNT(*) as count FROM EXECUTION_STATUSES"
        );
        assertEquals(6, ((Number) executionStatuses.get(0).get("count")).intValue());
    }

    @Test
    public void testIndexesCreated() {
        // Check that indexes were created
        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_SCHEMA = 'PUBLIC'",
                String.class
        );

        // Check for a few key indexes
        assertTrue(indexes.contains("IDX_TEMPLATE_PROJECT"));
        assertTrue(indexes.contains("IDX_VERSION_TEMPLATE"));
        assertTrue(indexes.contains("IDX_PARAMETER_VERSION"));
        assertTrue(indexes.contains("IDX_EXECUTION_VERSION"));
    }
}