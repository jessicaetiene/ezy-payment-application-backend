package br.com.ezy.ezypaymentapplicationbackend.api.webhook;

import io.swagger.v3.oas.annotations.media.Schema;

public record WebhookResponse(
        @Schema(description = "Webhook unique identifier", example = "6a05fe1f4fb87134417de9b5")
        String id,
        @Schema(description = "Url", example = "https://webhook")
        String url
) {
}
