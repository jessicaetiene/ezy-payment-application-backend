package br.com.ezy.ezypaymentapplicationbackend.api.payment;

import jakarta.validation.constraints.NotBlank;

public record PaymentResponse(
        String id,
        String firstName,
        String lastName,
        String zipCode
) {
}
