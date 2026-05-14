package br.com.ezy.ezypaymentapplicationbackend.api.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WebhookRequest(
        @NotBlank
        @Pattern(regexp = "https?://.+", message = "must be a valid HTTP or HTTPS URL")
        @Schema(description = "Url", example = "https://webhook")
        String url
) {
}
