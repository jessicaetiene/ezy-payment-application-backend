package br.com.ezy.ezypaymentapplicationbackend.application.webhook;

import br.com.ezy.ezypaymentapplicationbackend.api.webhook.WebhookRequest;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.Webhook;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateWebhookPaymentUseCase {
    private final WebhookRepository repository;

    public CreateWebhookPaymentUseCase(WebhookRepository repository) {
        this.repository = repository;
    }

    public Webhook execute(WebhookRequest webhookRequest){
        Webhook webhook = new Webhook(null, webhookRequest.url());
        return this.repository.save(webhook);
    }
}
