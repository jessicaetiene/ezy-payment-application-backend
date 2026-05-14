package br.com.ezy.ezypaymentapplicationbackend.config.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record ApiResponseError(
        @Schema(example = "2026-05-14T17:14:53.485420Z")
        Instant timestamp,
        @Schema(example = "400")
        Integer status,
        @Schema(example = "Bad Request")
        String error,
        @Schema(example = "Validation Failed")
        String message,
        @Schema(example = "/webhooks")
        String path,
        @Schema(example = "[url: must be a valid HTTP or HTTPS URL]")
        List<String> details
) {
}
