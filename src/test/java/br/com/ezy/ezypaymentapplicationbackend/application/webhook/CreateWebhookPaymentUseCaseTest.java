package br.com.ezy.ezypaymentapplicationbackend.application.webhook;

import br.com.ezy.ezypaymentapplicationbackend.api.webhook.WebhookRequest;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.Webhook;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateWebhookPaymentUseCaseTest {

    private final WebhookRepository webhookRepository = mock(WebhookRepository.class);
    private final CreateWebhookPaymentUseCase createWebhookPaymentUseCase = new CreateWebhookPaymentUseCase(webhookRepository);

    @Test
    void shouldSaveWebhook() {
        var request = new WebhookRequest("https://merchant.test/webhook");
        var savedWebhook = new Webhook("webhook-id", request.url());
        when(webhookRepository.save(new Webhook(null, request.url()))).thenReturn(savedWebhook);

        var result = createWebhookPaymentUseCase.execute(request);

        assertThat(result).isEqualTo(savedWebhook);
    }

    @Test
    void shouldPropagateRepositoryException() {
        var request = new WebhookRequest("https://merchant.test/webhook");
        var repositoryException = new RuntimeException("database unavailable");
        when(webhookRepository.save(new Webhook(null, request.url()))).thenThrow(repositoryException);

        assertThatThrownBy(() -> createWebhookPaymentUseCase.execute(request))
                .isSameAs(repositoryException);
        verify(webhookRepository).save(new Webhook(null, request.url()));
    }
}
