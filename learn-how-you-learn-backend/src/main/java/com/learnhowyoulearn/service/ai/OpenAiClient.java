package com.learnhowyoulearn.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhowyoulearn.client.openai.OpenAiRequest;
import com.learnhowyoulearn.client.openai.OpenAiResponse;
import com.learnhowyoulearn.config.OpenAiConfig;
import com.learnhowyoulearn.exception.AiGenerationException;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.net.SocketException;
import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(name = "openai.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient implements AiClient {

    private final WebClient.Builder webClientBuilder;
    private final OpenAiConfig openAiConfig;
    private final ObjectMapper objectMapper;

    @Override
    public AiResponse generate(AiRequest request) {
        long start = System.currentTimeMillis();
        try {
            String model = request.getModel() != null ? request.getModel() : openAiConfig.getModel();
            OpenAiRequest openAiRequest = OpenAiRequest.builder()
                    .model(model)
                    .temperature(request.getTemperature() != null ? request.getTemperature() : 0.3)
                    .maxTokens(request.getMaxTokens())
                    .messages(List.of(
                            OpenAiRequest.Message.builder().role("system").content(request.getSystemPrompt()).build(),
                            OpenAiRequest.Message.builder().role("user").content(request.getUserPrompt()).build()
                    ))
                    .build();

            int timeoutSecs = request.getTimeoutSeconds() != null
                    ? request.getTimeoutSeconds() : openAiConfig.getTimeoutSeconds();

            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                    .responseTimeout(Duration.ofSeconds(timeoutSecs));

            String rawJson = objectMapper.writeValueAsString(openAiRequest);
            log.info("Calling OpenAI for purpose={}, model={}", request.getPurpose(), model);

            OpenAiResponse response = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build()
                    .post()
                    .uri(openAiConfig.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + openAiConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(rawJson)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS,
                            resp -> Mono.error(new AiGenerationException("OpenAI rate limit hit. Wait a moment and try again.")))
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            resp -> resp.bodyToMono(String.class).flatMap(body ->
                                    Mono.error(new AiGenerationException("OpenAI error " + resp.statusCode().value() + ": " + body))))
                    .bodyToMono(OpenAiResponse.class)
                    .retryWhen(Retry.max(1).filter(e ->
                            e instanceof SocketException || (e.getCause() instanceof SocketException)))
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new AiGenerationException("OpenAI returned empty response");
            }

            String content = response.getChoices().get(0).getMessage().getContent();
            long latency = System.currentTimeMillis() - start;
            log.info("OpenAI call success: purpose={}, model={}, latencyMs={}", request.getPurpose(), openAiConfig.getModel(), latency);

            return AiResponse.builder()
                    .content(content)
                    .model(response.getModel() != null ? response.getModel() : openAiConfig.getModel())
                    .latencyMs(latency)
                    .rawResponse(content)
                    .build();

        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.error("OpenAI call failed: purpose={}, latencyMs={}, error={}", request.getPurpose(), latency, e.getMessage());
            throw new AiGenerationException("OpenAI API call failed: " + e.getMessage(), e);
        }
    }
}
