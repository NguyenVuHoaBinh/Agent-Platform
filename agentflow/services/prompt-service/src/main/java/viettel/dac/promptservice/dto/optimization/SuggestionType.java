package viettel.dac.promptservice.dto.optimization;

/**
 * Types of optimization suggestions
 */
public enum SuggestionType {
    /**
     * Suggestions for reducing token usage
     */
    TOKEN_EFFICIENCY,

    /**
     * Suggestions for improving clarity
     */
    CLARITY,

    /**
     * Suggestions for improving specificity
     */
    SPECIFICITY,

    /**
     * Suggestions for better error handling
     */
    ERROR_HANDLING,

    /**
     * Suggestions for better parameter usage
     */
    PARAMETER_USAGE,

    /**
     * Suggestions for improving response formatting
     */
    FORMATTING,

    /**
     * Suggestions for handling edge cases
     */
    EDGE_CASES,

    /**
     * Suggestions for improving grammar or language
     */
    GRAMMAR,

    /**
     * Suggestions for removing redundant content
     */
    REDUNDANCY,

    /**
     * Suggestions for improving prompt structure
     */
    STRUCTURE
}