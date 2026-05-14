package br.com.ezy.ezypaymentapplicationbackend.api.payment;

import br.com.ezy.ezypaymentapplicationbackend.application.payment.CreatePaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;

    public PaymentController(CreatePaymentUseCase createPaymentUseCase) {
        this.createPaymentUseCase = createPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest paymentRequest){
        var payment = this.createPaymentUseCase.execute(paymentRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new PaymentResponse(payment.id(), payment.firstName(), payment.lastName(), payment.zipCode()));
    }
}
