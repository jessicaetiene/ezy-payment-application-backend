package br.com.ezy.ezypaymentapplicationbackend.application.webhook;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEvent;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEventStatus;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

@Service
public class WebhookDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final WebhookEventRepository webhookEventRepository;
    private final RestClient restClient;
    private final int maxAttempts;

    public WebhookDispatcher(WebhookEventRepository webhookEventRepository,
                             RestClient.Builder restClientBuilder,
                             @Value("${webhooks.max-attempts:5}") int maxAttempts) {
        this.webhookEventRepository = webhookEventRepository;
        this.restClient = restClientBuilder.build();
        this.maxAttempts = maxAttempts;
    }

    @Async
    public void dispatchAsync(String eventId) {
        dispatch(eventId);
    }

    public void dispatch(String eventId) {
        webhookEventRepository.findById(eventId)
                .filter(event -> event.status() == WebhookEventStatus.PENDING)
                .ifPresent(this::dispatch);
    }

    private void dispatch(WebhookEvent event) {
        try {
            restClient.post()
                    .uri(event.webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event.payload())
                    .retrieve()
                    .toBodilessEntity();

            webhookEventRepository.save(event.delivered());
            LOGGER.info("Webhook event {} delivered to {}", event.id(), event.webhookUrl());
        } catch (Exception exception) {
            var errorMessage = exception.getMessage();
            if (event.attempts() + 1 >= maxAttempts) {
                webhookEventRepository.save(event.failed(errorMessage));
                LOGGER.error("Webhook event {} failed after {} attempts to {}: {}", event.id(), event.attempts() + 1,
                        event.webhookUrl(), errorMessage);
                return;
            }

            var nextAttemptAt = Instant.now().plus(backoff(event.attempts() + 1));
            webhookEventRepository.save(event.waitingForRetry(errorMessage, nextAttemptAt));
            LOGGER.warn("Webhook event {} delivery failed to {} and will retry at {}: {}", event.id(), event.webhookUrl(),
                    nextAttemptAt, errorMessage);
        }
    }

    private Duration backoff(int attempts) {
        return Duration.ofSeconds((long) Math.pow(2, attempts));
    }
}