package br.com.ezy.ezypaymentapplicationbackend.application.payment;

import br.com.ezy.ezypaymentapplicationbackend.api.payment.PaymentRequest;
import br.com.ezy.ezypaymentapplicationbackend.application.webhook.WebhookEventPublisher;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.service.CardEncryptorService;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreatePaymentUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookEventPublisher.class);
    private final PaymentRepository repository;
    private final CardEncryptorService cardEncryptorService;
    private final WebhookEventPublisher webhookEventPublisher;

    public CreatePaymentUseCase(PaymentRepository repository, CardEncryptorService cardEncryptorService, WebhookEventPublisher webhookEventPublisher) {
        this.repository = repository;
        this.cardEncryptorService = cardEncryptorService;
        this.webhookEventPublisher = webhookEventPublisher;
    }

    public Payment execute(PaymentRequest paymentRequest){
        var cardNumberEncrypted = cardEncryptorService.encrypt(paymentRequest.cardNumber());
        Payment payment = new Payment(null, paymentRequest.firstName(), paymentRequest.lastName(), paymentRequest.zipCode(), cardNumberEncrypted);
        var savedPayment = this.repository.save(payment);
        try {
            webhookEventPublisher.execute(savedPayment);
        } catch (Exception exception) {
            LOGGER.error("Payment {} was created, but webhook event publishing failed: {}", savedPayment.id(), exception.getMessage());
        }
        return savedPayment;
    }
}
