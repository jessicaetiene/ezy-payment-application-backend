package br.com.ezy.ezypaymentapplicationbackend.api.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PaymentResponse(
        @Schema(description = "Payment unique identifier", example = "6a0601867a3bf641f16744ec")
        String id,
        @Schema(description = "First Name", example = "John")
        String firstName,
        @Schema(description = "Last Name", example = "Doe")
        String lastName,
        @Schema(description = "Zip code", example = "01234-567")
        String zipCode
) {
}
