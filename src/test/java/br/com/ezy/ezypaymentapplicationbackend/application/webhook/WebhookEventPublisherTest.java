package br.com.ezy.ezypaymentapplicationbackend.application.webhook;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.Webhook;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEvent;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEventStatus;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookEventRepository;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookRepository;
import br.com.ezy.ezypaymentapplicationbackend.support.MongoTestContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
class WebhookEventPublisherTest extends MongoTestContainerSupport {

    @Autowired
    private WebhookEventPublisher webhookEventPublisher;

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @MockBean
    private WebhookDispatcher webhookDispatcher;

    @BeforeEach
    void setUp() {
        webhookEventRepository.deleteAll();
        webhookRepository.deleteAll();
    }

    @Test
    void shouldCreatePendingEventForEachRegisteredWebhookAndDispatchIt() {
        var firstWebhook = webhookRepository.save(new Webhook(null, "https://first.test/webhook"));
        var secondWebhook = webhookRepository.save(new Webhook(null, "https://second.test/webhook"));
        var payment = new Payment("payment-id", "Jessica", "Almeida", "01023-999", "encrypted-card");

        webhookEventPublisher.execute(payment);

        var events = webhookEventRepository.findAll();
        assertThat(events)
                .hasSize(2)
                .allSatisfy(event -> {
                    assertThat(event.paymentId()).isEqualTo(payment.id());
                    assertThat(event.payload()).isEqualTo(payment);
                    assertThat(event.status()).isEqualTo(WebhookEventStatus.PENDING);
                    assertThat(event.attempts()).isZero();
                    assertThat(event.nextAttemptAt()).isNotNull();
                    assertThat(event.lastError()).isNull();
                });
        assertThat(events)
                .extracting(WebhookEvent::webhookId, WebhookEvent::webhookUrl)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(firstWebhook.id(), firstWebhook.url()),
                        org.assertj.core.groups.Tuple.tuple(secondWebhook.id(), secondWebhook.url())
                );
        events.forEach(event -> verify(webhookDispatcher).dispatchAsync(event.id()));
    }

    @Test
    void shouldNotCreateEventsWhenThereAreNoRegisteredWebhooks() {
        var payment = new Payment("payment-id", "Jessica", "Almeida", "01023-999", "encrypted-card");

        webhookEventPublisher.execute(payment);

        assertThat(webhookEventRepository.findAll()).isEmpty();
        verifyNoInteractions(webhookDispatcher);
    }
}
