package br.com.ezy.ezypaymentapplicationbackend.application.webhook;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEvent;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEventStatus;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookEventRepository;
import br.com.ezy.ezypaymentapplicationbackend.support.MongoTestContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest
class WebhookRetrySchedulerTest extends MongoTestContainerSupport {

    @Autowired
    private WebhookRetryScheduler webhookRetryScheduler;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @MockBean
    private WebhookDispatcher webhookDispatcher;

    @BeforeEach
    void setUp() {
        webhookEventRepository.deleteAll();
    }

    @Test
    void shouldDispatchOnlyPendingEventsWithExpiredNextAttempt() {
        var now = Instant.now();
        var readyEvent = webhookEventRepository.save(event("ready", WebhookEventStatus.PENDING, now.minusSeconds(1)));
        webhookEventRepository.save(event("future", WebhookEventStatus.PENDING, now.plusSeconds(300)));
        webhookEventRepository.save(event("delivered", WebhookEventStatus.DELIVERED, now.minusSeconds(1)));
        webhookEventRepository.save(event("failed", WebhookEventStatus.FAILED, now.minusSeconds(1)));

        webhookRetryScheduler.retryPendingEvents();

        verify(webhookDispatcher).dispatchAsync(readyEvent.id());
        verifyNoMoreInteractions(webhookDispatcher);
    }

    private WebhookEvent event(String id, WebhookEventStatus status, Instant nextAttemptAt) {
        var payment = new Payment("payment-" + id, "Jessica", "Almeida", "01023-999", "encrypted-card");
        return new WebhookEvent(id, "webhook-" + id, "https://webhook.test/" + id, payment.id(), payment,
                status, 1, nextAttemptAt, null, null, null);
    }
}
