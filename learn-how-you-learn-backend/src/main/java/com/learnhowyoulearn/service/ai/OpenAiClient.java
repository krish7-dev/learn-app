package com.learnhowyoulearn.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnhowyoulearn.client.openai.OpenAiRequest;
import com.learnhowyoulearn.client.openai.OpenAiResponse;
import com.learnhowyoulearn.config.OpenAiConfig;
import com.learnhowyoulearn.exception.AiGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
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
            OpenAiRequest openAiRequest = OpenAiRequest.builder()
                    .model(openAiConfig.getModel())
                    .temperature(request.getTemperature() != null ? request.getTemperature() : 0.3)
                    .messages(List.of(
                            OpenAiRequest.Message.builder().role("system").content(request.getSystemPrompt()).build(),
                            OpenAiRequest.Message.builder().role("user").content(request.getUserPrompt()).build()
                    ))
                    .build();

            String rawJson = objectMapper.writeValueAsString(openAiRequest);
            log.debug("Calling OpenAI for purpose={}, model={}", request.getPurpose(), openAiConfig.getModel());

            OpenAiResponse response = webClientBuilder.build()
                    .post()
                    .uri(openAiConfig.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + openAiConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(rawJson)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .timeout(Duration.ofSeconds(openAiConfig.getTimeoutSeconds()))
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
