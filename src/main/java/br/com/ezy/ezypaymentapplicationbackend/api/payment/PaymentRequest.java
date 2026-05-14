package br.com.ezy.ezypaymentapplicationbackend.api.payment;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String zipCode,
        @NotBlank String cardNumber
) {
}
