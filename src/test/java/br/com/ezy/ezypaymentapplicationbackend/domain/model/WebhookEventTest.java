package br.com.ezy.ezypaymentapplicationbackend.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookEventTest {

    @Test
    void shouldCreateRetryEventIncreasingAttemptsAndPreservingPayload() {
        var event = event(WebhookEventStatus.PENDING, 2);
        var nextAttemptAt = Instant.parse("2026-05-15T10:15:30Z");

        var retryEvent = event.waitingForRetry("timeout", nextAttemptAt);

        assertThat(retryEvent.id()).isEqualTo(event.id());
        assertThat(retryEvent.status()).isEqualTo(WebhookEventStatus.PENDING);
        assertThat(retryEvent.attempts()).isEqualTo(3);
        assertThat(retryEvent.nextAttemptAt()).isEqualTo(nextAttemptAt);
        assertThat(retryEvent.lastError()).isEqualTo("timeout");
        assertThat(retryEvent.payload()).isEqualTo(event.payload());
    }

    @Test
    void shouldCreateDeliveredEventIncreasingAttemptsAndClearingRetryFields() {
        var event = event(WebhookEventStatus.PENDING, 0);

        var deliveredEvent = event.delivered();

        assertThat(deliveredEvent.status()).isEqualTo(WebhookEventStatus.DELIVERED);
        assertThat(deliveredEvent.attempts()).isEqualTo(1);
        assertThat(deliveredEvent.nextAttemptAt()).isNull();
        assertThat(deliveredEvent.lastError()).isNull();
    }

    @Test
    void shouldCreateFailedEventIncreasingAttemptsAndStoringError() {
        var event = event(WebhookEventStatus.PENDING, 4);

        var failedEvent = event.failed("internal server error");

        assertThat(failedEvent.status()).isEqualTo(WebhookEventStatus.FAILED);
        assertThat(failedEvent.attempts()).isEqualTo(5);
        assertThat(failedEvent.nextAttemptAt()).isNull();
        assertThat(failedEvent.lastError()).isEqualTo("internal server error");
    }

    private WebhookEvent event(WebhookEventStatus status, int attempts) {
        var payment = new Payment("payment-id", "Jessica", "Almeida", "01023-999", "encrypted-card");
        return new WebhookEvent("event-id", "webhook-id", "https://webhook.test", payment.id(), payment,
                status, attempts, Instant.parse("2026-05-15T10:00:00Z"), "previous-error",
                Instant.parse("2026-05-15T09:00:00Z"), Instant.parse("2026-05-15T09:30:00Z"));
    }
}
