package br.com.ezy.ezypaymentapplicationbackend.api.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CNPJ;

public record PaymentRequest(
        @Schema(
                description = "First Name",
                example = "John"
        )
        @NotBlank
        @NotBlank
        String firstName,

        @Schema(
                description = "Last Name",
                example = "Doe"
        )
        @NotBlank
        @NotBlank
        String lastName,

        @Schema(
                description = "Zip code",
                example = "01234-567"
        )
        @NotBlank
        @NotBlank
        String zipCode,
        @NotBlank String cardNumber
) {
}
