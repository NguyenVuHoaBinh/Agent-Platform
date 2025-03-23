package viettel.dac.promptservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import viettel.dac.promptservice.dto.response.ErrorResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void handleResourceNotFoundException() {
        // Arrange
        String errorMessage = "Resource not found";
        ResourceNotFoundException ex = new ResourceNotFoundException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleResourceNotFoundException(ex, webRequest);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertEquals(404, responseEntity.getBody().getStatus());
        assertEquals("RESOURCE_NOT_FOUND", responseEntity.getBody().getErrorCode());
        assertEquals(errorMessage, responseEntity.getBody().getMessage());
    }

    @Test
    public void handleResourceAlreadyExistsException() {
        // Arrange
        String errorMessage = "Resource already exists";
        ResourceAlreadyExistsException ex = new ResourceAlreadyExistsException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleResourceAlreadyExistsException(ex, webRequest);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        assertEquals(409, responseEntity.getBody().getStatus());
        assertEquals("RESOURCE_ALREADY_EXISTS", responseEntity.getBody().getErrorCode());
        assertEquals(errorMessage, responseEntity.getBody().getMessage());
    }

    @Test
    public void handleValidationExceptionWithoutErrors() {
        // Arrange
        String errorMessage = "Validation failed";
        ValidationException ex = new ValidationException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleValidationException(ex, webRequest);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(400, responseEntity.getBody().getStatus());
        assertEquals("VALIDATION_ERROR", responseEntity.getBody().getErrorCode());
        assertEquals(errorMessage, responseEntity.getBody().getMessage());
    }

    @Test
    public void handleValidationExceptionWithErrors() {
        // Arrange
        String errorMessage = "Validation failed";
        Map<String, String> errors = new HashMap<>();
        errors.put("field1", "Field1 error");
        errors.put("field2", "Field2 error");
        ValidationException ex = new ValidationException(errorMessage, errors);

        // Act
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleValidationException(ex, webRequest);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(400, responseEntity.getBody().getStatus());
        assertEquals("VALIDATION_ERROR", responseEntity.getBody().getErrorCode());
        assertEquals(errorMessage, responseEntity.getBody().getMessage());
        assertNotNull(responseEntity.getBody().getErrors());
        assertEquals(2, responseEntity.getBody().getErrors().size());
        assertEquals("Field1 error", responseEntity.getBody().getErrors().get("field1"));
        assertEquals("Field2 error", responseEntity.getBody().getErrors().get("field2"));
    }

    @Test
    public void handleMethodArgumentNotValidException() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> fieldErrors = new ArrayList<>();

        fieldErrors.add(new FieldError("object", "field1", "Field1 error"));
        fieldErrors.add(new FieldError("object", "field2", "Field2 error"));

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(new ArrayList<>(fieldErrors));

        // Act
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleMethodArgumentNotValidException(ex, webRequest);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(400, responseEntity.getBody().getStatus());
        assertEquals("VALIDATION_ERROR", responseEntity.getBody().getErrorCode());
        assertEquals("Input validation failed", responseEntity.getBody().getMessage());
        assertNotNull(responseEntity.getBody().getErrors());
        assertEquals(2, responseEntity.getBody().getErrors().size());
        assertEquals("Field1 error", responseEntity.getBody().getErrors().get("field1"));
        assertEquals("Field2 error", responseEntity.getBody().getErrors().get("field2"));
    }

    @Test
    public void handleConstraintViolationException() {
        // Arrange
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);

        Path path1 = mock(Path.class);
        Path path2 = mock(Path.class);

        when(path1.toString()).thenReturn("field1");
        when(path2.toString()).thenReturn("field2");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation1.getMessage()).thenReturn("Violation 1 message");
        when(violation2.getMessage()).thenReturn("Violation 2 message");

        violations.add(violation1);
        violations.add(violation2);

        ConstraintViolationException ex = new ConstraintViolationException("Constraint violations", violations);

        // Act
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleConstraintViolationException(ex, webRequest);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(400, responseEntity.getBody().getStatus());
        assertEquals("VALIDATION_ERROR", responseEntity.getBody().getErrorCode());
        assertEquals("Constraint violation", responseEntity.getBody().getMessage());
        assertNotNull(responseEntity.getBody().getErrors());
        assertEquals(2, responseEntity.getBody().getErrors().size());
        assertEquals("Violation 1 message", responseEntity.getBody().getErrors().get("field1"));
        assertEquals("Violation 2 message", responseEntity.getBody().getErrors().get("field2"));
    }

    @Test
    public void handleGlobalException() {
        // Arrange
        Exception ex = new RuntimeException("Unexpected error");

        // Act
        ResponseEntity<ErrorResponse> responseEntity = exceptionHandler.handleGlobalException(ex, webRequest);

        // Assert
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(500, responseEntity.getBody().getStatus());
        assertEquals("INTERNAL_SERVER_ERROR", responseEntity.getBody().getErrorCode());
        assertEquals("An unexpected error occurred", responseEntity.getBody().getMessage());
    }
}