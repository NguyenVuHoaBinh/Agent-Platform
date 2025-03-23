package viettel.dac.promptservice.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import viettel.dac.promptservice.model.enums.ExecutionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PromptExecutionTest {

    private PromptExecution execution;

    @BeforeEach
    void setUp() {
        execution = new PromptExecution();
        execution.setId("execution-1");
        execution.setProviderId("openai");
        execution.setModelId("gpt-4");
        execution.setStatus(ExecutionStatus.SUCCESS);
    }

    @Nested
    @DisplayName("Cost Calculation Tests")
    class CostCalculationTests {

        @Test
        @DisplayName("Should calculate cost correctly")
        void shouldCalculateCostCorrectly() {
            // Arrange
            execution.setInputTokens(1000);
            execution.setOutputTokens(500);

            BigDecimal inputCost = new BigDecimal("0.01");
            BigDecimal outputCost = new BigDecimal("0.02");

            // Act
            BigDecimal cost = execution.calculateCost(inputCost, outputCost);

            // Assert
            // Expected: (1000 * 0.01) + (500 * 0.02) = 10 + 10 = 20
            assertEquals(new BigDecimal("20.00"), cost);
        }

        @Test
        @DisplayName("Should return null for cost when token counts are null")
        void shouldReturnNullForCostWhenTokenCountsAreNull() {
            // Arrange
            execution.setInputTokens(null);
            execution.setOutputTokens(null);

            BigDecimal inputCost = new BigDecimal("0.01");
            BigDecimal outputCost = new BigDecimal("0.02");

            // Act
            BigDecimal cost = execution.calculateCost(inputCost, outputCost);

            // Assert
            assertNull(cost);
        }

        @Test
        @DisplayName("Should return null for cost with partially null token counts")
        void shouldReturnNullForCostWithPartiallyNullTokenCounts() {
            // Arrange
            execution.setInputTokens(1000);
            execution.setOutputTokens(null);

            BigDecimal inputCost = new BigDecimal("0.01");
            BigDecimal outputCost = new BigDecimal("0.02");

            // Act
            BigDecimal cost = execution.calculateCost(inputCost, outputCost);

            // Assert
            assertNull(cost);
        }
    }

    @Nested
    @DisplayName("Execution Time Tests")
    class ExecutionTimeTests {

        @Test
        @DisplayName("Should set metrics from execution time")
        void shouldSetMetricsFromExecutionTime() {
            // Arrange
            LocalDateTime startTime = LocalDateTime.now().minusSeconds(2);
            LocalDateTime endTime = LocalDateTime.now();

            // Act
            execution.setMetricsFromExecutionTime(startTime, endTime);

            // Assert
            assertEquals(startTime, execution.getExecutedAt());
            assertTrue(execution.getResponseTimeMs() >= 2000);
        }

        @Test
        @DisplayName("Should handle null times in metric calculation")
        void shouldHandleNullTimesInMetricCalculation() {
            // Arrange, Act
            execution.setMetricsFromExecutionTime(null, LocalDateTime.now());

            // Assert
            assertNull(execution.getResponseTimeMs());

            // Arrange, Act again
            execution.setMetricsFromExecutionTime(LocalDateTime.now(), null);

            // Assert again
            assertNull(execution.getResponseTimeMs());
        }
    }

    @Nested
    @DisplayName("Status Tests")
    class StatusTests {

        @Test
        @DisplayName("Should check status correctly")
        void shouldCheckStatusCorrectly() {
            // Test isSuccessful
            execution.setStatus(ExecutionStatus.SUCCESS);
            assertTrue(execution.isSuccessful());

            execution.setStatus(ExecutionStatus.ERROR);
            assertFalse(execution.isSuccessful());

            // Test isTimeout
            execution.setStatus(ExecutionStatus.TIMEOUT);
            assertTrue(execution.isTimeout());

            execution.setStatus(ExecutionStatus.SUCCESS);
            assertFalse(execution.isTimeout());

            // Test hasValidationErrors
            execution.setStatus(ExecutionStatus.INVALID_PARAMS);
            assertTrue(execution.hasValidationErrors());

            execution.setStatus(ExecutionStatus.SUCCESS);
            assertFalse(execution.hasValidationErrors());
        }
    }

    @Nested
    @DisplayName("Token Rate Tests")
    class TokenRateTests {

        @Test
        @DisplayName("Should calculate tokens per second correctly")
        void shouldCalculateTokensPerSecondCorrectly() {
            // Arrange
            execution.setTokenCount(1000);
            execution.setResponseTimeMs(2000L); // 2 seconds

            // Act
            Double tokenRate = execution.getTokensPerSecond();

            // Assert
            assertEquals(500.0, tokenRate); // 1000 tokens / 2 seconds = 500 tokens/sec
        }

        @Test
        @DisplayName("Should handle null values in token rate calculation")
        void shouldHandleNullValuesInTokenRateCalculation() {
            // Arrange & Act & Assert - Null token count
            execution.setTokenCount(null);
            execution.setResponseTimeMs(2000L);
            assertNull(execution.getTokensPerSecond());

            // Arrange & Act & Assert - Null response time
            execution.setTokenCount(1000);
            execution.setResponseTimeMs(null);
            assertNull(execution.getTokensPerSecond());
        }

        @Test
        @DisplayName("Should handle zero response time in token rate calculation")
        void shouldHandleZeroResponseTimeInTokenRateCalculation() {
            // Arrange
            execution.setTokenCount(1000);
            execution.setResponseTimeMs(0L);

            // Act & Assert
            assertNull(execution.getTokensPerSecond());
        }
    }
}