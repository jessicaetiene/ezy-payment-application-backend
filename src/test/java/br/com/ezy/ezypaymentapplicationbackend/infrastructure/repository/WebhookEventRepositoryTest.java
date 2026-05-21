package br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEvent;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEventStatus;
import br.com.ezy.ezypaymentapplicationbackend.support.MongoTestContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.time.Instant;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(WebhookEventRepositoryTest.MongoAuditingTestConfig.class)
class WebhookEventRepositoryTest extends MongoTestContainerSupport {

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @BeforeEach
    void setUp() {
        webhookEventRepository.deleteAll();
    }

    @Test
    void shouldFindOnlyTopTenPendingEventsReadyForRetryOrderedByCreatedAt() {
        var now = Instant.parse("2026-05-15T12:00:00Z");
        IntStream.rangeClosed(1, 12).forEach(index -> webhookEventRepository.save(event(
                "ready-" + index,
                WebhookEventStatus.PENDING,
                now.minusSeconds(1),
                now.minusSeconds(100 - index)
        )));
        webhookEventRepository.save(event("future", WebhookEventStatus.PENDING, now.plusSeconds(60), now.minusSeconds(200)));
        webhookEventRepository.save(event("delivered", WebhookEventStatus.DELIVERED, now.minusSeconds(1), now.minusSeconds(300)));
        webhookEventRepository.save(event("failed", WebhookEventStatus.FAILED, now.minusSeconds(1), now.minusSeconds(400)));

        var readyEvents = webhookEventRepository.findTop10ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                WebhookEventStatus.PENDING,
                now
        );

        assertThat(readyEvents)
                .hasSize(10)
                .extracting(WebhookEvent::id)
                .containsExactly("ready-1", "ready-2", "ready-3", "ready-4", "ready-5", "ready-6", "ready-7", "ready-8", "ready-9", "ready-10");
    }

    @EnableMongoAuditing
    static class MongoAuditingTestConfig {
    }

    private WebhookEvent event(String id, WebhookEventStatus status, Instant nextAttemptAt, Instant createdAt) {
        var payment = new Payment("payment-" + id, "Jessica", "Almeida", "01023-999", "encrypted-card");
        return new WebhookEvent(id, "webhook-" + id, "https://webhook.test/" + id, payment.id(), payment,
                status, 1, nextAttemptAt, null, createdAt, createdAt);
    }
}
