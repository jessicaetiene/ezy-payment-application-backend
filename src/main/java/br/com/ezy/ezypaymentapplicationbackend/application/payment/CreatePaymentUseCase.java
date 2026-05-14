package br.com.ezy.ezypaymentapplicationbackend.application.payment;

import br.com.ezy.ezypaymentapplicationbackend.api.payment.PaymentRequest;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.service.CardEncryptorService;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class CreatePaymentUseCase {
    private final PaymentRepository repository;
    private final CardEncryptorService cardEncryptorService;

    public CreatePaymentUseCase(PaymentRepository repository, CardEncryptorService cardEncryptorService) {
        this.repository = repository;
        this.cardEncryptorService = cardEncryptorService;
    }

    public Payment execute(PaymentRequest paymentRequest){
        var cardNumberEncrypted = cardEncryptorService.encrypt(paymentRequest.cardNumber());
        Payment payment = new Payment(null, paymentRequest.firstName(), paymentRequest.lastName(), paymentRequest.zipCode(), cardNumberEncrypted);
        return this.repository.save(payment);
    }
}
