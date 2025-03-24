package viettel.dac.promptservice.service.optimization;

import viettel.dac.promptservice.dto.optimization.OptimizationResult;
import viettel.dac.promptservice.dto.optimization.PromptOptimizationRequest;
import viettel.dac.promptservice.dto.optimization.SuggestionType;
import viettel.dac.promptservice.model.entity.PromptVersion;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for prompt optimization operations
 */
public interface PromptOptimizationService {

    /**
     * Analyze a prompt version and provide optimization suggestions
     *
     * @param versionId Version ID
     * @return Optimization suggestions
     */
    OptimizationResult analyzePrompt(String versionId);

    /**
     * Generate optimized variations of a prompt
     *
     * @param versionId Version ID
     * @param suggestionTypes Types of suggestions to apply
     * @param count Number of variations to generate
     * @return Map of variation text to performance metrics
     */
    Map<String, Map<String, Object>> generateOptimizedVariations(
            String versionId,
            List<SuggestionType> suggestionTypes,
            int count);

    /**
     * Run a complete optimization process asynchronously
     *
     * @param versionId Version ID
     * @param request Optimization parameters
     * @return Future with optimization results
     */
    CompletableFuture<OptimizationResult> optimizePromptAsync(
            String versionId,
            PromptOptimizationRequest request);

    /**
     * Create a new version with optimized prompt
     *
     * @param sourceVersionId Source version ID
     * @param optimizedText Optimized prompt text
     * @return New version with optimized prompt
     */
    PromptVersion createOptimizedVersion(String sourceVersionId, String optimizedText);

    /**
     * Generate optimization job for batch processing
     *
     * @param versionId Version ID
     * @param request Optimization parameters
     * @return Job ID
     */
    String createOptimizationJob(String versionId, PromptOptimizationRequest request);

    /**
     * Apply combined optimization strategies
     *
     * @param versionId Version ID
     * @param strategies Map of strategy names to enabled flags
     * @return Optimized prompt text
     */
    String applyOptimizationStrategies(String versionId, Map<String, Boolean> strategies);

    /**
     * Suggest improvements for token efficiency
     *
     * @param versionId Version ID
     * @return Optimization suggestions
     */
    OptimizationResult suggestTokenEfficiencyImprovements(String versionId);

    /**
     * Suggest improvements for clarity and specificity
     *
     * @param versionId Version ID
     * @return Optimization suggestions
     */
    OptimizationResult suggestClarityImprovements(String versionId);

    /**
     * Suggest improvements for error handling
     *
     * @param versionId Version ID
     * @return Optimization suggestions
     */
    OptimizationResult suggestErrorHandlingImprovements(String versionId);

    /**
     * Suggest parameter improvements
     *
     * @param versionId Version ID
     * @return Optimization suggestions
     */
    OptimizationResult suggestParameterImprovements(String versionId);
}