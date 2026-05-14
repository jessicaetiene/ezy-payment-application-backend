package br.com.ezy.ezypaymentapplicationbackend.application.webhook;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEvent;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEventStatus;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookEventRepository;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WebhookEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookEventPublisher.class);
    private final WebhookRepository webhookRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDispatcher webhookDispatcher;

    public WebhookEventPublisher(WebhookRepository webhookRepository, WebhookEventRepository webhookEventRepository, WebhookDispatcher webhookDispatcher) {
        this.webhookRepository = webhookRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.webhookDispatcher = webhookDispatcher;
    }

    public void execute(Payment payment){
        webhookRepository.findAll().forEach(
                webhook -> {
                    var event = new WebhookEvent(null, webhook.id(), webhook.url(), payment.id(), payment,
                            WebhookEventStatus.PENDING, 0, Instant.now(), null, null, null);
                    var savedEvent = webhookEventRepository.save(event);
                    LOGGER.info("Webhook event {} queued for payment {} and webhook {}", savedEvent.id(), payment.id(), webhook.id());
                    webhookDispatcher.dispatchAsync(savedEvent.id());
                });
    }
}
