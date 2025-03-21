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
 * Implementation of the LlmProvider interface for OpenAI's API
 */
@Slf4j
public class OpenAiProvider extends BaseLlmProvider {

    private static final String PROVIDER_ID = "openai";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> availableModels;
    private final Map<String, Integer> maxContextLengths;
    private final Map<String, double[]> modelCosts; // [input cost per 1K tokens, output cost per 1K tokens]

    public OpenAiProvider(WebClient.Builder webClientBuilder, String apiKey, Executor executor) {
        super(executor);
        this.webClient = webClientBuilder
                .baseUrl(API_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.objectMapper = new ObjectMapper();

        // Initialize available models
        this.availableModels = new HashMap<>();
        availableModels.put("gpt-4-turbo", "GPT-4 Turbo");
        availableModels.put("gpt-4", "GPT-4");
        availableModels.put("gpt-3.5-turbo", "GPT-3.5 Turbo");
        availableModels.put("gpt-3.5-turbo-16k", "GPT-3.5 Turbo 16K");

        // Initialize context lengths
        this.maxContextLengths = new HashMap<>();
        maxContextLengths.put("gpt-4-turbo", 128000);
        maxContextLengths.put("gpt-4", 8192);
        maxContextLengths.put("gpt-3.5-turbo", 4096);
        maxContextLengths.put("gpt-3.5-turbo-16k", 16384);

        // Initialize costs per 1K tokens [input, output]
        this.modelCosts = new HashMap<>();
        modelCosts.put("gpt-4-turbo", new double[]{0.01, 0.03});
        modelCosts.put("gpt-4", new double[]{0.03, 0.06});
        modelCosts.put("gpt-3.5-turbo", new double[]{0.0015, 0.002});
        modelCosts.put("gpt-3.5-turbo-16k", new double[]{0.003, 0.004});
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
        log.debug("Executing prompt with OpenAI, model: {}", request.getModelId());

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
                        log.error("OpenAI API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                        throw mapToProviderException(e, request.getModelId());
                    })
                    .onErrorResume(e -> {
                        if (e instanceof LlmProviderException) {
                            return Mono.error(e);
                        }
                        log.error("Error calling OpenAI API: {}", e.getMessage());
                        return Mono.error(new LlmProviderException(
                                "OpenAI API call failed: " + e.getMessage(),
                                e, PROVIDER_ID, request.getModelId(),
                                LlmProviderException.ErrorType.UNKNOWN));
                    })
                    .block();

            JsonNode responseNode = objectMapper.readTree(response);
            LlmResponse llmResponse = parseResponse(responseNode, request, startTime, inputTokenCount);
            log.debug("Successfully executed prompt with OpenAI, model: {}, tokens: {}",
                    request.getModelId(), llmResponse.getTotalTokenCount());

            return llmResponse;

        } catch (LlmProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error executing prompt with OpenAI: {}", e.getMessage(), e);
            throw new LlmProviderException(
                    "Failed to execute prompt with OpenAI: " + e.getMessage(),
                    e, PROVIDER_ID, request.getModelId(),
                    LlmProviderException.ErrorType.UNKNOWN);
        }
    }

    @Override
    public int countTokens(String prompt, String modelId) {
        // Simple approximation - 4 characters per token
        // In a production environment, you should use a proper tokenizer
        // like GPT2Tokenizer from TikToken library
        return Math.max(1, prompt.length() / 4);
    }

    @Override
    public double calculateCost(int inputTokens, int outputTokens, String modelId) {
        if (!modelCosts.containsKey(modelId)) {
            return 0.0;
        }

        double[] costs = modelCosts.get(modelId);
        double inputCost = (inputTokens / 1000.0) * costs[0];
        double outputCost = (outputTokens / 1000.0) * costs[1];

        return inputCost + outputCost;
    }

    @Override
    public int getMaxContextLength(String modelId) {
        return maxContextLengths.getOrDefault(modelId, 4096);
    }

    /**
     * Create the JSON request body for the OpenAI API
     */
    private ObjectNode createRequestBody(LlmRequest request) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", request.getModelId());

        // Determine system prompt to use
        String systemPrompt = DEFAULT_SYSTEM_PROMPT;
        if (StringUtils.hasText(request.getSystemPrompt())) {
            systemPrompt = request.getSystemPrompt();
        }

        // Add messages array with system and user messages
        requestBody.putArray("messages")
                .add(objectMapper.createObjectNode()
                        .put("role", "system")
                        .put("content", systemPrompt))
                .add(objectMapper.createObjectNode()
                        .put("role", "user")
                        .put("content", request.getPrompt()));

        // Add common parameters
        requestBody.put("max_tokens", request.getMaxTokens());
        requestBody.put("temperature", request.getTemperature());
        requestBody.put("top_p", request.getTopP());
        requestBody.put("n", request.getN());

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
     * Parse the OpenAI API response into our LlmResponse format
     */
    private LlmResponse parseResponse(JsonNode responseNode, LlmRequest request,
                                      LocalDateTime startTime, int inputTokenCount) {

        List<String> choices = new ArrayList<>();
        String primaryText = "";

        if (responseNode.has("choices") && responseNode.get("choices").isArray()) {
            JsonNode choicesNode = responseNode.get("choices");
            for (int i = 0; i < choicesNode.size(); i++) {
                JsonNode choice = choicesNode.get(i);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String text = choice.get("message").get("content").asText();
                    if (i == 0) {
                        primaryText = text;
                    } else {
                        choices.add(text);
                    }
                }
            }
        }

        int outputTokenCount = 0;
        int totalTokenCount = 0;

        if (responseNode.has("usage")) {
            JsonNode usageNode = responseNode.get("usage");
            inputTokenCount = usageNode.has("prompt_tokens") ?
                    usageNode.get("prompt_tokens").asInt() : inputTokenCount;
            outputTokenCount = usageNode.has("completion_tokens") ?
                    usageNode.get("completion_tokens").asInt() : 0;
            totalTokenCount = usageNode.has("total_tokens") ?
                    usageNode.get("total_tokens").asInt() : (inputTokenCount + outputTokenCount);
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
        if (responseNode.has("created")) {
            metadata.put("created_timestamp", responseNode.get("created").asLong());
        }

        responseBuilder.metadata(metadata);

        return responseBuilder.build();
    }

    /**
     * Map WebClient exceptions to our provider exception
     */
    private LlmProviderException mapToProviderException(WebClientResponseException e, String modelId) {
        LlmProviderException.ErrorType errorType;
        String message = "OpenAI API error: " + e.getStatusCode();

        try {
            JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString());
            if (errorNode.has("error") && errorNode.get("error").has("message")) {
                message = errorNode.get("error").get("message").asText();
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
                    if (message.contains("context length") || message.contains("token limit")) {
                        yield LlmProviderException.ErrorType.CONTEXT_LENGTH;
                    } else if (message.contains("content filter") || message.contains("content policy")) {
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