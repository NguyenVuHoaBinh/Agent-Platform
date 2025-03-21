package viettel.dac.promptservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO representing a request to an LLM provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {

    /**
     * The provider ID (e.g., "openai", "anthropic")
     */
    private String providerId;

    /**
     * The model ID (e.g., "gpt-4-turbo", "claude-3-opus")
     */
    private String modelId;

    /**
     * The prompt text with parameter substitutions applied
     */
    private String prompt;

    /**
     * The system prompt to guide the LLM's behavior
     */
    private String systemPrompt;

    /**
     * The maximum number of tokens to generate in the response
     */
    @Builder.Default
    private Integer maxTokens = 1024;

    /**
     * Temperature controls randomness (0.0 to 1.0)
     * Lower values make output more deterministic
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * Top-p (nucleus) sampling
     * Controls diversity by considering only the most likely tokens
     */
    @Builder.Default
    private Double topP = 1.0;

    /**
     * Number of response alternatives to generate
     */
    @Builder.Default
    private Integer n = 1;

    /**
     * Provider-specific parameters
     */
    @Builder.Default
    private Map<String, Object> extraParams = new HashMap<>();

    /**
     * Execution timeout in milliseconds
     */
    @Builder.Default
    private long timeoutMs = 30000;
}