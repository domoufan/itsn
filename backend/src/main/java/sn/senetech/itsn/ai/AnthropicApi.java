package sn.senetech.itsn.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Représentation minimale des messages de l'API Anthropic (Messages API). */
public final class AnthropicApi {
    private AnthropicApi() {}

    public record Tool(String name, String description, @JsonProperty("input_schema") Map<String, Object> inputSchema) {}

    public record Request(String model, @JsonProperty("max_tokens") int maxTokens, String system,
                          List<Tool> tools, List<Map<String, Object>> messages, double temperature) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text, String id, String name, Map<String, Object> input) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(@JsonProperty("input_tokens") int inputTokens, @JsonProperty("output_tokens") int outputTokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(String id, String model, List<ContentBlock> content, @JsonProperty("stop_reason") String stopReason, Usage usage) {}
}
