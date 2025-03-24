package viettel.dac.promptservice.service.optimization.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.promptservice.dto.optimization.OptimizationResult;
import viettel.dac.promptservice.dto.optimization.PromptOptimizationRequest;
import viettel.dac.promptservice.dto.optimization.SuggestionType;
import viettel.dac.promptservice.dto.request.BatchJobRequest;
import viettel.dac.promptservice.dto.request.PromptParameterRequest;
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.event.BatchJobCreationEvent;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.BatchJobType;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.service.PromptVersionService;
import viettel.dac.promptservice.service.llm.LlmService;
import viettel.dac.promptservice.service.optimization.PromptOptimizationService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of prompt optimization service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptOptimizationServiceImpl implements PromptOptimizationService {

    private final PromptVersionRepository versionRepository;
    private final PromptVersionService versionService;
    private final LlmService llmService;
    private final ApplicationEventPublisher eventPublisher;

    // Default provider and model IDs for optimization
    private static final String DEFAULT_PROVIDER_ID = "openai";
    private static final String DEFAULT_MODEL_ID = "gpt-4";

    // Regex patterns for analysis
    private static final Pattern REDUNDANT_TEXT_PATTERN = Pattern.compile("\\b(please|kindly|would you|could you)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern VAGUE_TERMS_PATTERN = Pattern.compile("\\b(good|nice|better|improve|enhance)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    @Override
    public OptimizationResult analyzePrompt(String versionId) {
        log.debug("Analyzing prompt version: {}", versionId);

        PromptVersion version = getVersion(versionId);
        String promptText = version.getContent();

        // Create result container
        OptimizationResult result = OptimizationResult.builder()
                .originalText(promptText)
                .suggestions(new ArrayList<>())
                .build();

        // Analyze for token efficiency
        analyzeTokenEfficiency(promptText, result);

        // Analyze for clarity
        analyzeClarity(promptText, result);

        // Analyze parameter usage
        analyzeParameterUsage(version, result);

        // Analyze error handling
        analyzeErrorHandling(promptText, result);

        // Calculate overall score
        result.calculateScore();

        // Set recommendation based on score
        if (result.getScore() < 50) {
            result.setRecommendation("This prompt has significant room for improvement. Consider applying the suggested optimizations.");
        } else if (result.getScore() < 80) {
            result.setRecommendation("This prompt could benefit from some optimizations. Review the suggestions and apply as appropriate.");
        } else {
            result.setRecommendation("This prompt is well-optimized. Minor improvements may still be possible.");
        }

        return result;
    }

    @Override
    @Async
    public CompletableFuture<OptimizationResult> optimizePromptAsync(String versionId, PromptOptimizationRequest request) {
        log.debug("Starting asynchronous optimization for version: {}", versionId);

        try {
            // Analyze the prompt
            OptimizationResult result = analyzePrompt(versionId);

            // Filter suggestions by requested types
            List<OptimizationResult.Suggestion> filteredSuggestions = result.getSuggestions().stream()
                    .filter(s -> request.getSuggestionTypes().contains(s.getType()))
                    .toList();

            result.setSuggestions(filteredSuggestions);

            // Apply automatic optimization if requested
            if (request.isApplyAutomatically() && !filteredSuggestions.isEmpty()) {
                PromptVersion version = getVersion(versionId);
                String optimizedText = applyAllSuggestions(version.getContent(), filteredSuggestions);
                result.setOptimizedText(optimizedText);
                result.setAutomaticallyOptimized(true);

                // Create new version if requested
                if (request.isCreateNewVersion()) {
                    createOptimizedVersion(versionId, optimizedText);
                }
            }

            // Recalculate score based on filtered suggestions
            result.calculateScore();

            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("Error during async optimization", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String createOptimizationJob(String versionId, PromptOptimizationRequest request) {
        log.debug("Creating optimization job for version: {}", versionId);

        // Get the original version
        PromptVersion originalVersion = getVersion(versionId);

        // Create a batch job request
        BatchJobRequest jobRequest = BatchJobRequest.builder()
                .name("Prompt Optimization: " + originalVersion.getVersionNumber())
                .description("Automatic optimization of prompt: " + originalVersion.getVersionNumber())
                .jobType(BatchJobType.PROMPT_OPTIMIZATION)
                .parameters(Map.of(
                        "versionId", versionId,
                        "providerId", request.getProviderId() != null ? request.getProviderId() : DEFAULT_PROVIDER_ID,
                        "modelId", request.getModelId() != null ? request.getModelId() : DEFAULT_MODEL_ID,
                        "suggestionTypes", request.getSuggestionTypes().stream()
                                .map(SuggestionType::name)
                                .toList()
                ))
                .startImmediately(true)
                .build();

        // Publish event instead of directly calling the service
        eventPublisher.publishEvent(new BatchJobCreationEvent(this, jobRequest));

        // Return a placeholder job ID - the actual ID will be assigned by the job service
        return "job-pending-" + UUID.randomUUID();
    }

    /**
     * Apply all suggestions to the original text
     */
    private String applyAllSuggestions(String originalText, List<OptimizationResult.Suggestion> suggestions) {
        // Sort suggestions by position (descending) to avoid index changes
        List<OptimizationResult.Suggestion> sortedSuggestions = new ArrayList<>(suggestions);
        sortedSuggestions.sort((a, b) -> {
            if (a.getLocation() == null || b.getLocation() == null) {
                return 0;
            }
            return Integer.compare(b.getLocation().getStartIndex(), a.getLocation().getStartIndex());
        });

        // Apply each suggestion
        String modifiedText = originalText;
        for (OptimizationResult.Suggestion suggestion : sortedSuggestions) {
            if (suggestion.getLocation() != null && suggestion.getSuggestedText() != null) {
                modifiedText = modifiedText.substring(0, suggestion.getLocation().getStartIndex()) +
                        suggestion.getSuggestedText() +
                        modifiedText.substring(suggestion.getLocation().getEndIndex());
            }
        }

        return modifiedText;
    }

    @Override
    @Transactional
    public PromptVersion createOptimizedVersion(String sourceVersionId, String optimizedText) {
        log.debug("Creating optimized version from source: {}", sourceVersionId);

        PromptVersion sourceVersion = getVersion(sourceVersionId);

        // Create new version request using builder pattern
        PromptVersionRequest request = PromptVersionRequest.builder()
                .templateId(sourceVersion.getTemplate().getId())
                .content(optimizedText)
                .systemPrompt(sourceVersion.getSystemPrompt())
                .parentVersionId(sourceVersionId)
                .parameters(sourceVersion.getParameters().stream()
                        .map(p -> {
                            return PromptParameterRequest.builder()
                                    .name(p.getName())
                                    .description(p.getDescription())
                                    .required(p.isRequired())
                                    .defaultValue(p.getDefaultValue())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .build();

        // Create the new version
        return versionService.createVersion(request);
    }

    @Override
    public Map<String, Map<String, Object>> generateOptimizedVariations(
            String versionId, List<SuggestionType> suggestionTypes, int count) {
        log.debug("Generating {} optimized variations for version: {}", count, versionId);

        // Implementation for generating variations
        Map<String, Map<String, Object>> result = new HashMap<>();
        // TO BE IMPLEMENTED
        return result;
    }

    @Override
    public String applyOptimizationStrategies(String versionId, Map<String, Boolean> strategies) {
        log.debug("Applying optimization strategies to version: {}", versionId);

        // Implementation for applying combined strategies
        // TO BE IMPLEMENTED
        return "Optimized text would go here";
    }

    @Override
    public OptimizationResult suggestTokenEfficiencyImprovements(String versionId) {
        log.debug("Suggesting token efficiency improvements for version: {}", versionId);

        // Create a focused result for token efficiency
        OptimizationResult result = OptimizationResult.builder()
                .originalText(getVersion(versionId).getContent())
                .suggestions(new ArrayList<>())
                .build();

        analyzeTokenEfficiency(result.getOriginalText(), result);
        result.calculateScore();
        return result;
    }

    @Override
    public OptimizationResult suggestClarityImprovements(String versionId) {
        log.debug("Suggesting clarity improvements for version: {}", versionId);

        // Create a focused result for clarity
        OptimizationResult result = OptimizationResult.builder()
                .originalText(getVersion(versionId).getContent())
                .suggestions(new ArrayList<>())
                .build();

        analyzeClarity(result.getOriginalText(), result);
        result.calculateScore();
        return result;
    }

    @Override
    public OptimizationResult suggestErrorHandlingImprovements(String versionId) {
        log.debug("Suggesting error handling improvements for version: {}", versionId);

        // Create a focused result for error handling
        OptimizationResult result = OptimizationResult.builder()
                .originalText(getVersion(versionId).getContent())
                .suggestions(new ArrayList<>())
                .build();

        analyzeErrorHandling(result.getOriginalText(), result);
        result.calculateScore();
        return result;
    }

    @Override
    public OptimizationResult suggestParameterImprovements(String versionId) {
        log.debug("Suggesting parameter improvements for version: {}", versionId);

        // Create a focused result for parameters
        OptimizationResult result = OptimizationResult.builder()
                .originalText(getVersion(versionId).getContent())
                .suggestions(new ArrayList<>())
                .build();

        analyzeParameterUsage(getVersion(versionId), result);
        result.calculateScore();
        return result;
    }

    /**
     * Get a prompt version by ID
     */
    private PromptVersion getVersion(String versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + versionId));
    }

    /**
     * Analyze prompt for token efficiency
     */
    private void analyzeTokenEfficiency(String promptText, OptimizationResult result) {
        // Check for redundant language
        Matcher redundancyMatcher = REDUNDANT_TEXT_PATTERN.matcher(promptText);
        while (redundancyMatcher.find()) {
            String redundantText = redundancyMatcher.group();
            result.addSuggestion(OptimizationResult.Suggestion.builder()
                    .type(SuggestionType.TOKEN_EFFICIENCY)
                    .description("Redundant courtesy phrase")
                    .severity(OptimizationResult.Severity.MINOR)
                    .originalText(redundantText)
                    .suggestedText("")
                    .location(OptimizationResult.TextLocation.builder()
                            .startIndex(redundancyMatcher.start())
                            .endIndex(redundancyMatcher.end())
                            .build())
                    .explanation("Removing courtesy phrases can reduce token count without affecting functionality.")
                    .build());
        }

        // Check for lengthy sections
        String[] paragraphs = promptText.split("\n\n");
        for (int i = 0; i < paragraphs.length; i++) {
            if (paragraphs[i].length() > 300) {
                result.addSuggestion(OptimizationResult.Suggestion.builder()
                        .type(SuggestionType.TOKEN_EFFICIENCY)
                        .description("Lengthy paragraph")
                        .severity(OptimizationResult.Severity.MINOR)
                        .originalText(paragraphs[i])
                        .explanation("Consider breaking down or condensing lengthy paragraphs to reduce token usage.")
                        .build());
            }
        }

        // Check for repetitive instructions
        // This is a simplified implementation; a more robust approach would use semantic analysis
        Set<String> sentences = new HashSet<>();
        String[] sentenceSplits = promptText.split("[.!?]\\s*");
        for (String sentence : sentenceSplits) {
            String normalized = sentence.trim().toLowerCase();
            if (normalized.length() > 20 && !sentences.add(normalized)) {
                result.addSuggestion(OptimizationResult.Suggestion.builder()
                        .type(SuggestionType.REDUNDANCY)
                        .description("Potentially repetitive instruction")
                        .severity(OptimizationResult.Severity.MINOR)
                        .originalText(sentence)
                        .explanation("This instruction appears to be repeated, which increases token count unnecessarily.")
                        .build());
            }
        }
    }

    /**
     * Analyze prompt for clarity and specificity
     */
    private void analyzeClarity(String promptText, OptimizationResult result) {
        // Check for vague terms
        Matcher vagueMatcher = VAGUE_TERMS_PATTERN.matcher(promptText);
        while (vagueMatcher.find()) {
            String vagueText = vagueMatcher.group();
            result.addSuggestion(OptimizationResult.Suggestion.builder()
                    .type(SuggestionType.CLARITY)
                    .description("Vague or subjective term")
                    .severity(OptimizationResult.Severity.MINOR)
                    .originalText(vagueText)
                    .location(OptimizationResult.TextLocation.builder()
                            .startIndex(vagueMatcher.start())
                            .endIndex(vagueMatcher.end())
                            .build())
                    .explanation("Replace vague terms with specific, measurable criteria for better results.")
                    .build());
        }

        // Check for missing structure
        if (!promptText.contains("\n")) {
            result.addSuggestion(OptimizationResult.Suggestion.builder()
                    .type(SuggestionType.STRUCTURE)
                    .description("Lack of structure")
                    .severity(OptimizationResult.Severity.MAJOR)
                    .explanation("Adding structure with sections or numbered steps can improve clarity and response quality.")
                    .build());
        }

        // Check for overlong sentences (potential clarity issues)
        String[] sentences = promptText.split("[.!?]\\s*");
        for (String sentence : sentences) {
            if (sentence.length() > 200) {
                result.addSuggestion(OptimizationResult.Suggestion.builder()
                        .type(SuggestionType.CLARITY)
                        .description("Overlong sentence")
                        .severity(OptimizationResult.Severity.MINOR)
                        .originalText(sentence)
                        .explanation("Breaking down long sentences can improve clarity and precision of the response.")
                        .build());
            }
        }
    }

    /**
     * Analyze prompt for parameter usage
     */
    private void analyzeParameterUsage(PromptVersion version, OptimizationResult result) {
        String promptText = version.getContent();

        // Extract parameter references from prompt
        Set<String> referencedParams = new HashSet<>();
        Matcher paramMatcher = PARAMETER_PATTERN.matcher(promptText);
        while (paramMatcher.find()) {
            referencedParams.add(paramMatcher.group(1));
        }

        // Get defined parameters
        Set<String> definedParams = version.getParameters().stream()
                .map(param -> param.getName())
                .collect(java.util.stream.Collectors.toSet());

        // Find undefined referenced parameters
        for (String param : referencedParams) {
            if (!definedParams.contains(param)) {
                result.addSuggestion(OptimizationResult.Suggestion.builder()
                        .type(SuggestionType.PARAMETER_USAGE)
                        .description("Undefined parameter reference")
                        .severity(OptimizationResult.Severity.CRITICAL)
                        .originalText("{{" + param + "}}")
                        .explanation("This parameter is referenced in the prompt but not defined in parameter list.")
                        .build());
            }
        }

        // Find unused defined parameters
        for (String param : definedParams) {
            if (!referencedParams.contains(param)) {
                result.addSuggestion(OptimizationResult.Suggestion.builder()
                        .type(SuggestionType.PARAMETER_USAGE)
                        .description("Unused parameter")
                        .severity(OptimizationResult.Severity.MAJOR)
                        .explanation("Parameter '" + param + "' is defined but not used in the prompt text.")
                        .build());
            }
        }
    }

    /**
     * Analyze prompt for error handling
     */
    private void analyzeErrorHandling(String promptText, OptimizationResult result) {
        // Check for error handling instructions
        boolean hasErrorHandling = promptText.toLowerCase().contains("error") ||
                promptText.toLowerCase().contains("invalid") ||
                promptText.toLowerCase().contains("cannot") ||
                promptText.toLowerCase().contains("if you're unable");

        if (!hasErrorHandling) {
            result.addSuggestion(OptimizationResult.Suggestion.builder()
                    .type(SuggestionType.ERROR_HANDLING)
                    .description("Missing error handling instructions")
                    .severity(OptimizationResult.Severity.MAJOR)
                    .explanation("Add instructions for how the model should respond when it cannot fulfill the request.")
                    .build());
        }

        // Check for edge case handling
        boolean hasEdgeCaseHandling = promptText.toLowerCase().contains("edge case") ||
                promptText.toLowerCase().contains("special case") ||
                promptText.toLowerCase().contains("if there are no") ||
                promptText.toLowerCase().contains("if none");

        if (!hasEdgeCaseHandling) {
            result.addSuggestion(OptimizationResult.Suggestion.builder()
                    .type(SuggestionType.EDGE_CASES)
                    .description("Missing edge case handling")
                    .severity(OptimizationResult.Severity.MINOR)
                    .explanation("Consider adding instructions for how to handle edge cases or unusual inputs.")
                    .build());
        }
    }
}