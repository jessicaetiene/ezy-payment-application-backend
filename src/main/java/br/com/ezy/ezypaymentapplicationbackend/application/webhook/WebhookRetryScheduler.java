package br.com.ezy.ezypaymentapplicationbackend.application.webhook;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEventStatus;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class WebhookRetryScheduler {
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDispatcher webhookDispatcher;

    public WebhookRetryScheduler(WebhookEventRepository webhookEventRepository, WebhookDispatcher webhookDispatcher) {
        this.webhookEventRepository = webhookEventRepository;
        this.webhookDispatcher = webhookDispatcher;
    }

    @Scheduled(fixedDelayString = "${webhooks.retry-delay-ms:30000}")
    public void retryPendingEvents() {
        webhookEventRepository.findTop10ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(WebhookEventStatus.PENDING, Instant.now())
                .forEach(event -> webhookDispatcher.dispatchAsync(event.id()));
    }
}
