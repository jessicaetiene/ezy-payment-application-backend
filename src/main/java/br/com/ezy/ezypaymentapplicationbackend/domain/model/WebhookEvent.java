package br.com.ezy.ezypaymentapplicationbackend.domain.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "webhook_events")
public record WebhookEvent(
        @Id
        String id,
        String webhookId,
        String webhookUrl,
        String paymentId,
        Payment payload,
        WebhookEventStatus status,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        @CreatedDate
        Instant createdAt,
        @LastModifiedDate
        Instant updatedAt
) {
    public WebhookEvent waitingForRetry(String errorMessage, Instant nextAttemptAt) {
        return new WebhookEvent(id, webhookId, webhookUrl, paymentId, payload, WebhookEventStatus.PENDING, attempts + 1,
                nextAttemptAt, errorMessage, createdAt, updatedAt);
    }

    public WebhookEvent delivered() {
        return new WebhookEvent(id, webhookId, webhookUrl, paymentId, payload, WebhookEventStatus.DELIVERED, attempts + 1,
                null, null, createdAt, updatedAt);
    }

    public WebhookEvent failed(String errorMessage) {
        return new WebhookEvent(id, webhookId, webhookUrl, paymentId, payload, WebhookEventStatus.FAILED, attempts + 1,
                null, errorMessage, createdAt, updatedAt);
    }
}