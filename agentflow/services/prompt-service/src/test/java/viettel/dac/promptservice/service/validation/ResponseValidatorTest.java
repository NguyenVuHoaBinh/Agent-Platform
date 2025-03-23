package viettel.dac.promptservice.service.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import viettel.dac.promptservice.dto.validation.ValidationResult;
import viettel.dac.promptservice.service.validation.ResponseValidator;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseValidatorTest {

    private ResponseValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new ResponseValidator();
    }

    @Test
    public void testValidateResponse_NullResponse() {
        ValidationResult result = validator.validateResponse(null, Collections.singletonMap("contains", "test"));

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals("empty_response", result.getIssues().get(0).getRule());
        assertEquals(ValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
    }

    @Test
    public void testValidateResponse_EmptyResponse() {
        ValidationResult result = validator.validateResponse("", Collections.singletonMap("contains", "test"));

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals("empty_response", result.getIssues().get(0).getRule());
        assertEquals(ValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
    }

    @Test
    public void testValidateResponse_NullCriteria() {
        ValidationResult result = validator.validateResponse("test response", null);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
    }

    @Test
    public void testValidateResponse_EmptyCriteria() {
        ValidationResult result = validator.validateResponse("test response", Collections.emptyMap());

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
    }

    @Test
    public void testValidateResponse_Contains_SingleString_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", "test");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("contains").isPassed());
    }

    @Test
    public void testValidateResponse_Contains_SingleString_Fail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", "missing");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals("contains", result.getIssues().get(0).getRule());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("contains").isPassed());
    }

    @Test
    public void testValidateResponse_Contains_MultipleStrings_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", Arrays.asList("test", "response"));

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("contains").isPassed());
    }

    @Test
    public void testValidateResponse_Contains_MultipleStrings_PartialFail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", Arrays.asList("test", "missing"));

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("contains").isPassed());
    }

    @Test
    public void testValidateResponse_Contains_InvalidFormat() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", 123); // Not a string or list

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals("contains", result.getIssues().get(0).getRule());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("contains").isPassed());
    }

    @Test
    public void testValidateResponse_NotContains_SingleString_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("not_contains", "missing");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("not_contains").isPassed());
    }

    @Test
    public void testValidateResponse_NotContains_SingleString_Fail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("not_contains", "test");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals("not_contains", result.getIssues().get(0).getRule());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("not_contains").isPassed());
    }

    @Test
    public void testValidateResponse_NotContains_MultipleStrings_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("not_contains", Arrays.asList("missing1", "missing2"));

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("not_contains").isPassed());
    }

    @Test
    public void testValidateResponse_NotContains_MultipleStrings_PartialFail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("not_contains", Arrays.asList("missing", "test"));

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("not_contains").isPassed());
    }

    @Test
    public void testValidateResponse_RegexMatch_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("regex_match", ".*test.*");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("regex_match").isPassed());
    }

    @Test
    public void testValidateResponse_RegexMatch_Fail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("regex_match", "^test.*");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("regex_match").isPassed());
    }

    @Test
    public void testValidateResponse_RegexMatch_InvalidRegex() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("regex_match", "[invalid regex");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("regex_match").isPassed());
    }

    @Test
    public void testValidateResponse_MinLength_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("min_length", "10");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("min_length").isPassed());
    }

    @Test
    public void testValidateResponse_MinLength_Fail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("min_length", "30");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("min_length").isPassed());
    }

    @Test
    public void testValidateResponse_MinLength_InvalidFormat() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("min_length", "not-a-number");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals("min_length", result.getIssues().get(0).getRule());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("min_length").isPassed());
    }

    @Test
    public void testValidateResponse_MaxLength_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("max_length", "30");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("max_length").isPassed());
    }

    @Test
    public void testValidateResponse_MaxLength_Fail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("max_length", "10");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("max_length").isPassed());
    }

    @Test
    public void testValidateResponse_JsonFormat_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("json_format", true);

        ValidationResult result = validator.validateResponse("{\"key\": \"value\"}", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("json_format").isPassed());
    }

    @Test
    public void testValidateResponse_JsonFormat_Fail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("json_format", true);

        ValidationResult result = validator.validateResponse("This is not JSON", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("json_format").isPassed());
    }

    @Test
    public void testValidateResponse_Structure_Sections_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        Map<String, Object> structure = new HashMap<>();
        structure.put("sections", Arrays.asList("Introduction", "Conclusion"));
        criteria.put("structure", structure);

        String response = "# Introduction\nThis is an introduction.\n\n# Conclusion\nThis is a conclusion.";
        ValidationResult result = validator.validateResponse(response, criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("structure").isPassed());
    }

    @Test
    public void testValidateResponse_Structure_Sections_Fail() {
        Map<String, Object> criteria = new HashMap<>();
        Map<String, Object> structure = new HashMap<>();
        structure.put("sections", Arrays.asList("Introduction", "Missing Section"));
        criteria.put("structure", structure);

        String response = "# Introduction\nThis is an introduction.\n\n# Conclusion\nThis is a conclusion.";
        ValidationResult result = validator.validateResponse(response, criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("structure").isPassed());
    }

    @Test
    public void testValidateResponse_Structure_Headers_Pass() {
        Map<String, Object> criteria = new HashMap<>();
        Map<String, Object> structure = new HashMap<>();
        structure.put("headers", Arrays.asList("Introduction", "Conclusion"));
        criteria.put("structure", structure);

        String response = "# Introduction\nThis is an introduction.\n\n# Conclusion\nThis is a conclusion.";
        ValidationResult result = validator.validateResponse(response, criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("structure").isPassed());
    }

    @Test
    public void testValidateResponse_Structure_Headers_Fail() {
        Map<String, Object> criteria = new HashMap<>();
        Map<String, Object> structure = new HashMap<>();
        structure.put("headers", Arrays.asList("Introduction", "Missing Header"));
        criteria.put("structure", structure);

        String response = "# Introduction\nThis is an introduction.\n\n# Conclusion\nThis is a conclusion.";
        ValidationResult result = validator.validateResponse(response, criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("structure").isPassed());
    }

    @Test
    public void testValidateResponse_Structure_InvalidFormat() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("structure", "not-a-map");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals("structure", result.getIssues().get(0).getRule());
        assertEquals(1, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("structure").isPassed());
    }

    @Test
    public void testValidateResponse_UnknownRule() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("unknown_rule", "value");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed()); // Unknown rules should not fail validation
        assertEquals(1.0, result.getScore());
        assertEquals(1, result.getIssues().size());
        assertEquals("unknown_rule", result.getIssues().get(0).getRule());
        assertEquals(ValidationResult.ValidationSeverity.WARNING, result.getIssues().get(0).getSeverity());
        assertEquals(0, result.getRuleResults().size());
    }

    @Test
    public void testValidateResponse_MultipleCriteria_AllPass() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", "test");
        criteria.put("min_length", "10");
        criteria.put("max_length", "30");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());
        assertEquals(0, result.getIssues().size());
        assertEquals(3, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("contains").isPassed());
        assertTrue(result.getRuleResults().get("min_length").isPassed());
        assertTrue(result.getRuleResults().get("max_length").isPassed());
    }

    @Test
    public void testValidateResponse_MultipleCriteria_SomeFail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", "test");
        criteria.put("min_length", "100"); // This will fail
        criteria.put("max_length", "30");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(2.0/3.0, result.getScore(), 0.01); // 2 out of 3 rules passed
        assertEquals(1, result.getIssues().size());
        assertEquals(3, result.getRuleResults().size());
        assertTrue(result.getRuleResults().get("contains").isPassed());
        assertFalse(result.getRuleResults().get("min_length").isPassed());
        assertTrue(result.getRuleResults().get("max_length").isPassed());
    }

    @Test
    public void testValidateResponse_MultipleCriteria_AllFail() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", "missing");
        criteria.put("min_length", "100");
        criteria.put("max_length", "10");

        ValidationResult result = validator.validateResponse("This is a test response", criteria);

        assertFalse(result.isPassed());
        assertEquals(0.0, result.getScore());
        assertEquals(3, result.getIssues().size());
        assertEquals(3, result.getRuleResults().size());
        assertFalse(result.getRuleResults().get("contains").isPassed());
        assertFalse(result.getRuleResults().get("min_length").isPassed());
        assertFalse(result.getRuleResults().get("max_length").isPassed());
    }
}
