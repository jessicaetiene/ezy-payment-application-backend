package br.com.ezy.ezypaymentapplicationbackend.application.payment;

import br.com.ezy.ezypaymentapplicationbackend.api.payment.PaymentRequest;
import br.com.ezy.ezypaymentapplicationbackend.application.webhook.WebhookEventPublisher;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.service.CardEncryptorService;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.PaymentRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreatePaymentUseCaseTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final CardEncryptorService cardEncryptorService = mock(CardEncryptorService.class);
    private final WebhookEventPublisher webhookEventPublisher = mock(WebhookEventPublisher.class);
    private final CreatePaymentUseCase createPaymentUseCase = new CreatePaymentUseCase(
            paymentRepository,
            cardEncryptorService,
            webhookEventPublisher
    );

    @Test
    void shouldEncryptCardSavePaymentAndPublishWebhookEvents() {
        var request = request();
        var savedPayment = new Payment("payment-id", request.firstName(), request.lastName(), request.zipCode(), "encrypted-card");
        when(cardEncryptorService.encrypt(request.cardNumber())).thenReturn("encrypted-card");
        when(paymentRepository.save(new Payment(null, request.firstName(), request.lastName(), request.zipCode(), "encrypted-card")))
                .thenReturn(savedPayment);

        var result = createPaymentUseCase.execute(request);

        assertThat(result).isEqualTo(savedPayment);
        verify(webhookEventPublisher).execute(savedPayment);
    }

    @Test
    void shouldReturnSavedPaymentWhenWebhookPublishingFails() {
        var request = request();
        var savedPayment = new Payment("payment-id", request.firstName(), request.lastName(), request.zipCode(), "encrypted-card");
        when(cardEncryptorService.encrypt(request.cardNumber())).thenReturn("encrypted-card");
        when(paymentRepository.save(new Payment(null, request.firstName(), request.lastName(), request.zipCode(), "encrypted-card")))
                .thenReturn(savedPayment);
        doThrow(new RuntimeException("publisher unavailable")).when(webhookEventPublisher).execute(savedPayment);

        var result = createPaymentUseCase.execute(request);

        assertThat(result).isEqualTo(savedPayment);
    }

    @Test
    void shouldNotSavePaymentWhenEncryptionFails() {
        var request = request();
        when(cardEncryptorService.encrypt(request.cardNumber())).thenThrow(new IllegalStateException("Error encrypting card number"));

        assertThatThrownBy(() -> createPaymentUseCase.execute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error encrypting card number");
        verifyNoInteractions(paymentRepository, webhookEventPublisher);
    }

    private PaymentRequest request() {
        return new PaymentRequest("Jessica", "Almeida", "01023-999", "4111111111111111");
    }
}
