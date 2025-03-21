package viettel.dac.promptservice.service.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for LLM providers
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LlmProviderProperties {

    private final OpenAi openai = new OpenAi();
    private final Anthropic anthropic = new Anthropic();
    private Integer defaultTimeoutMs = 30000;
    private Integer defaultMaxTokens = 1024;
    private Double defaultTemperature = 0.7;
    private Integer maxRetries = 3;
    private Integer retryDelayMs = 1000;

    /**
     * OpenAI provider configuration
     */
    @Data
    public static class OpenAi {
        private boolean enabled = false;
        private String apiKey;
        private String defaultModel = "gpt-3.5-turbo";
        private String apiUrl = "https://api.openai.com/v1";
        private String organizationId;
    }

    /**
     * Anthropic provider configuration
     */
    @Data
    public static class Anthropic {
        private boolean enabled = false;
        private String apiKey;
        private String defaultModel = "claude-3-sonnet-20240229";
        private String apiUrl = "https://api.anthropic.com/v1";
        private String apiVersion = "2023-06-01";
    }
}