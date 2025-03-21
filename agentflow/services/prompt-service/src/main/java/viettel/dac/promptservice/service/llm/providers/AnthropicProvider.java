package viettel.dac.promptservice.service.llm.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import viettel.dac.promptservice.dto.request.LlmRequest;
import viettel.dac.promptservice.dto.response.LlmResponse;
import viettel.dac.promptservice.exception.LlmProviderException;
import viettel.dac.promptservice.service.llm.BaseLlmProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Implementation of the LlmProvider interface for Anthropic's Claude API
 */
@Slf4j
public class AnthropicProvider extends BaseLlmProvider {

    private static final String PROVIDER_ID = "anthropic";
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final String DEFAULT_SYSTEM_PROMPT = "You are Claude, a helpful AI assistant.";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> availableModels;
    private final Map<String, Integer> maxContextLengths;
    private final Map<String, double[]> modelCosts; // [input cost per 1M tokens, output cost per 1M tokens]



    public AnthropicProvider(WebClient.Builder webClientBuilder, String apiKey, Executor executor) {
        super(executor);
        this.webClient = webClientBuilder
                .baseUrl(API_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", API_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.objectMapper = new ObjectMapper();

        // Initialize available models
        this.availableModels = new HashMap<>();
        availableModels.put("claude-3-opus-20240229", "Claude 3 Opus");
        availableModels.put("claude-3-sonnet-20240229", "Claude 3 Sonnet");
        availableModels.put("claude-3-haiku-20240307", "Claude 3 Haiku");
        availableModels.put("claude-2.1", "Claude 2.1");
        availableModels.put("claude-2.0", "Claude 2.0");
        availableModels.put("claude-instant-1.2", "Claude Instant 1.2");

        // Initialize context lengths
        this.maxContextLengths = new HashMap<>();
        maxContextLengths.put("claude-3-opus-20240229", 200000);
        maxContextLengths.put("claude-3-sonnet-20240229", 200000);
        maxContextLengths.put("claude-3-haiku-20240307", 200000);
        maxContextLengths.put("claude-2.1", 100000);
        maxContextLengths.put("claude-2.0", 100000);
        maxContextLengths.put("claude-instant-1.2", 100000);

        // Initialize costs per 1M tokens [input, output]
        this.modelCosts = new HashMap<>();
        modelCosts.put("claude-3-opus-20240229", new double[]{15.0, 75.0});
        modelCosts.put("claude-3-sonnet-20240229", new double[]{3.0, 15.0});
        modelCosts.put("claude-3-haiku-20240307", new double[]{0.25, 1.25});
        modelCosts.put("claude-2.1", new double[]{8.0, 24.0});
        modelCosts.put("claude-2.0", new double[]{8.0, 24.0});
        modelCosts.put("claude-instant-1.2", new double[]{1.63, 5.51});
    }

    @Override
    public String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    public Map<String, String> getAvailableModels() {
        return availableModels;
    }

    @Override
    public LlmResponse executePrompt(LlmRequest request) throws LlmProviderException {
        LocalDateTime startTime = LocalDateTime.now();
        log.debug("Executing prompt with Anthropic, model: {}", request.getModelId());

        validateRequest(request);

        try {
            ObjectNode requestBody = createRequestBody(request);

            // Calculate token counts before sending request
            int inputTokenCount = countTokens(request.getPrompt(), request.getModelId());

            String response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(request.getTimeoutMs()))
                    .onErrorResume(WebClientResponseException.class, e -> {
                        log.error("Anthropic API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                        throw mapToProviderException(e, request.getModelId());
                    })
                    .onErrorResume(e -> {
                        if (e instanceof LlmProviderException) {
                            return Mono.error(e);
                        }
                        log.error("Error calling Anthropic API: {}", e.getMessage());
                        return Mono.error(new LlmProviderException(
                                "Anthropic API call failed: " + e.getMessage(),
                                e, PROVIDER_ID, request.getModelId(),
                                LlmProviderException.ErrorType.UNKNOWN));
                    })
                    .block();

            JsonNode responseNode = objectMapper.readTree(response);
            LlmResponse llmResponse = parseResponse(responseNode, request, startTime, inputTokenCount);
            log.debug("Successfully executed prompt with Anthropic, model: {}, tokens: {}",
                    request.getModelId(), llmResponse.getTotalTokenCount());

            return llmResponse;

        } catch (LlmProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing prompt with Anthropic: {}", e.getMessage(), e);
            throw new LlmProviderException(
                    "Failed to execute prompt with Anthropic: " + e.getMessage(),
                    e, PROVIDER_ID, request.getModelId(),
                    LlmProviderException.ErrorType.UNKNOWN);
        }
    }

    @Override
    public int countTokens(String prompt, String modelId) {
        // Simple approximation - 4 characters per token
        // In a production environment, you should use a proper tokenizer
        return Math.max(1, prompt.length() / 4);
    }

    @Override
    public double calculateCost(int inputTokens, int outputTokens, String modelId) {
        if (!modelCosts.containsKey(modelId)) {
            return 0.0;
        }

        double[] costs = modelCosts.get(modelId);
        double inputCost = (inputTokens / 1000000.0) * costs[0];
        double outputCost = (outputTokens / 1000000.0) * costs[1];

        return inputCost + outputCost;
    }

    @Override
    public int getMaxContextLength(String modelId) {
        return maxContextLengths.getOrDefault(modelId, 100000);
    }

    /**
     * Create the JSON request body for the Anthropic API
     */
    private ObjectNode createRequestBody(LlmRequest request) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", request.getModelId());

        // Determine system prompt to use
        String systemPrompt = DEFAULT_SYSTEM_PROMPT;
        if (StringUtils.hasText(request.getSystemPrompt())) {
            systemPrompt = request.getSystemPrompt();
        }

        // Add system prompt
        requestBody.put("system", systemPrompt);

        // Add message array with user message
        requestBody.putArray("messages")
                .add(objectMapper.createObjectNode()
                        .put("role", "user")
                        .put("content", request.getPrompt()));

        // Add common parameters
        requestBody.put("max_tokens", request.getMaxTokens());
        requestBody.put("temperature", request.getTemperature());
        requestBody.put("top_p", request.getTopP());

        // Add any extra parameters
        if (request.getExtraParams() != null) {
            request.getExtraParams().forEach((key, value) -> {
                if (value instanceof Boolean) {
                    requestBody.put(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    requestBody.put(key, (Integer) value);
                } else if (value instanceof Double) {
                    requestBody.put(key, (Double) value);
                } else if (value instanceof String) {
                    requestBody.put(key, (String) value);
                }
            });
        }

        return requestBody;
    }

    /**
     * Parse the Anthropic API response into our LlmResponse format
     */
    private LlmResponse parseResponse(JsonNode responseNode, LlmRequest request,
                                      LocalDateTime startTime, int inputTokenCount) {

        List<String> choices = new ArrayList<>();
        String primaryText = "";

        if (responseNode.has("content") && responseNode.get("content").isArray()) {
            JsonNode contentNode = responseNode.get("content");
            for (int i = 0; i < contentNode.size(); i++) {
                JsonNode item = contentNode.get(i);
                if (item.has("type") && "text".equals(item.get("type").asText()) &&
                        item.has("text")) {
                    primaryText = item.get("text").asText();
                    break;
                }
            }
        }

        int outputTokenCount = 0;
        int totalTokenCount = 0;

        if (responseNode.has("usage")) {
            JsonNode usageNode = responseNode.get("usage");
            inputTokenCount = usageNode.has("input_tokens") ?
                    usageNode.get("input_tokens").asInt() : inputTokenCount;
            outputTokenCount = usageNode.has("output_tokens") ?
                    usageNode.get("output_tokens").asInt() : 0;
            totalTokenCount = inputTokenCount + outputTokenCount;
        } else {
            // Fallback if usage info is not available
            outputTokenCount = countTokens(primaryText, request.getModelId());
            totalTokenCount = inputTokenCount + outputTokenCount;
        }

        double cost = calculateCost(inputTokenCount, outputTokenCount, request.getModelId());

        LlmResponse.LlmResponseBuilder responseBuilder = createBaseResponse(request, startTime)
                .text(primaryText)
                .alternatives(choices)
                .inputTokenCount(inputTokenCount)
                .outputTokenCount(outputTokenCount)
                .totalTokenCount(totalTokenCount)
                .cost(cost)
                .rawResponse(responseNode.toString())
                .successful(true);

        Map<String, Object> metadata = new HashMap<>();
        if (responseNode.has("id")) {
            metadata.put("response_id", responseNode.get("id").asText());
        }
        if (responseNode.has("model")) {
            metadata.put("model", responseNode.get("model").asText());
        }
        if (responseNode.has("stop_reason")) {
            metadata.put("stop_reason", responseNode.get("stop_reason").asText());
        }

        responseBuilder.metadata(metadata);

        return responseBuilder.build();
    }

    /**
     * Map WebClient exceptions to our provider exception
     */
    private LlmProviderException mapToProviderException(WebClientResponseException e, String modelId) {
        LlmProviderException.ErrorType errorType;
        String message = "Anthropic API error: " + e.getStatusCode();

        try {
            JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString());
            if (errorNode.has("error") && errorNode.get("error").has("message")) {
                message = errorNode.get("error").get("message").asText();
            } else if (errorNode.has("error")) {
                message = errorNode.get("error").asText();
            }
        } catch (Exception ex) {
            // Use status message if we can't parse the error
            message += " " + e.getStatusText();
        }

        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            errorType = LlmProviderException.ErrorType.UNKNOWN;
        } else {
            errorType = switch (status) {
                case UNAUTHORIZED -> LlmProviderException.ErrorType.AUTHENTICATION;
                case TOO_MANY_REQUESTS -> LlmProviderException.ErrorType.RATE_LIMIT;
                case BAD_REQUEST -> {
                    if (message.contains("context window") || message.contains("token limit")) {
                        yield LlmProviderException.ErrorType.CONTEXT_LENGTH;
                    } else if (message.contains("content policy") || message.contains("content filter")) {
                        yield LlmProviderException.ErrorType.CONTENT_FILTER;
                    } else {
                        yield LlmProviderException.ErrorType.INVALID_REQUEST;
                    }
                }
                case SERVICE_UNAVAILABLE, INTERNAL_SERVER_ERROR ->
                        LlmProviderException.ErrorType.SERVICE_UNAVAILABLE;
                default -> LlmProviderException.ErrorType.UNKNOWN;
            };
        }



        return new LlmProviderException(message, e, PROVIDER_ID, modelId, errorType);
    }
}