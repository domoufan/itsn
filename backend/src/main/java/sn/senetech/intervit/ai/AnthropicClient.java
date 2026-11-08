package sn.senetech.intervit.ai;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sn.senetech.intervit.config.AppProperties;

import java.time.Duration;

/** Client HTTP vers l'API Claude. La clé n'est lue que depuis l'environnement et n'apparaît jamais dans les journaux. */
@Component
public class AnthropicClient {

    private final RestClient restClient;
    private final AppProperties.Ai ai;

    public AnthropicClient(AppProperties properties) {
        this.ai = properties.ai();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(Math.max(5, ai.timeoutSeconds())));
        this.restClient = RestClient.builder()
            .baseUrl(ai.baseUrl())
            .requestFactory(factory)
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public AnthropicApi.Response createMessage(AnthropicApi.Request request) {
        return restClient.post()
            .uri("/v1/messages")
            .header("x-api-key", ai.apiKey())
            .body(request)
            .retrieve()
            .body(AnthropicApi.Response.class);
    }
}
