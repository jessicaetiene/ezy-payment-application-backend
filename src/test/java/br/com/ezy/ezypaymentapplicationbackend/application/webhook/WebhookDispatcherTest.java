package br.com.ezy.ezypaymentapplicationbackend.application.webhook;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEvent;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEventStatus;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookEventRepository;
import br.com.ezy.ezypaymentapplicationbackend.support.MongoTestContainerSupport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "webhooks.max-attempts=5")
class WebhookDispatcherTest extends MongoTestContainerSupport {

    @Autowired
    private WebhookDispatcher webhookDispatcher;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    private HttpServer httpServer;
    private AtomicInteger receivedRequests;

    @BeforeEach
    void setUp() throws IOException {
        webhookEventRepository.deleteAll();
        receivedRequests = new AtomicInteger();
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.start();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void shouldMarkPendingEventAsDeliveredWhenWebhookReturnsSuccess() {
        httpServer.createContext("/success", exchange -> respond(exchange, 204));
        var event = webhookEventRepository.save(event("success", WebhookEventStatus.PENDING, 0, url("/success")));

        webhookDispatcher.dispatch(event.id());

        var updatedEvent = webhookEventRepository.findById(event.id()).orElseThrow();
        assertThat(updatedEvent.status()).isEqualTo(WebhookEventStatus.DELIVERED);
        assertThat(updatedEvent.attempts()).isEqualTo(1);
        assertThat(updatedEvent.nextAttemptAt()).isNull();
        assertThat(updatedEvent.lastError()).isNull();
        assertThat(receivedRequests).hasValue(1);
    }

    @Test
    void shouldScheduleRetryWhenWebhookReturnsErrorAndAttemptsAreAvailable() {
        httpServer.createContext("/server-error", exchange -> respond(exchange, 500));
        var event = webhookEventRepository.save(event("retry", WebhookEventStatus.PENDING, 0, url("/server-error")));
        var beforeDispatch = Instant.now();

        webhookDispatcher.dispatch(event.id());

        var updatedEvent = webhookEventRepository.findById(event.id()).orElseThrow();
        assertThat(updatedEvent.status()).isEqualTo(WebhookEventStatus.PENDING);
        assertThat(updatedEvent.attempts()).isEqualTo(1);
        assertThat(updatedEvent.nextAttemptAt()).isAfter(beforeDispatch.plusSeconds(1));
        assertThat(updatedEvent.lastError()).contains("500");
        assertThat(receivedRequests).hasValue(1);
    }

    @Test
    void shouldMarkEventAsFailedWhenMaxAttemptsIsReached() {
        httpServer.createContext("/server-error", exchange -> respond(exchange, 500));
        var event = webhookEventRepository.save(event("failed", WebhookEventStatus.PENDING, 4, url("/server-error")));

        webhookDispatcher.dispatch(event.id());

        var updatedEvent = webhookEventRepository.findById(event.id()).orElseThrow();
        assertThat(updatedEvent.status()).isEqualTo(WebhookEventStatus.FAILED);
        assertThat(updatedEvent.attempts()).isEqualTo(5);
        assertThat(updatedEvent.nextAttemptAt()).isNull();
        assertThat(updatedEvent.lastError()).contains("500");
        assertThat(receivedRequests).hasValue(1);
    }

    @Test
    void shouldIgnoreEventsThatAreNotPending() {
        var event = webhookEventRepository.save(event("delivered", WebhookEventStatus.DELIVERED, 1, url("/success")));

        webhookDispatcher.dispatch(event.id());

        var unchangedEvent = webhookEventRepository.findById(event.id()).orElseThrow();
        assertThat(unchangedEvent).isEqualTo(event);
        assertThat(receivedRequests).hasValue(0);
    }

    @Test
    void shouldIgnoreUnknownEventId() {
        webhookDispatcher.dispatch("unknown-id");

        assertThat(webhookEventRepository.findAll()).isEmpty();
        assertThat(receivedRequests).hasValue(0);
    }

    private WebhookEvent event(String id, WebhookEventStatus status, int attempts, String webhookUrl) {
        var payment = new Payment("payment-" + id, "Jessica", "Almeida", "01023-999", "encrypted-card");
        return new WebhookEvent(id, "webhook-" + id, webhookUrl, payment.id(), payment,
                status, attempts, Instant.now(), null, null, null);
    }

    private String url(String path) {
        return "http://localhost:" + httpServer.getAddress().getPort() + path;
    }

    private void respond(HttpExchange exchange, int status) throws IOException {
        receivedRequests.incrementAndGet();
        var body = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
