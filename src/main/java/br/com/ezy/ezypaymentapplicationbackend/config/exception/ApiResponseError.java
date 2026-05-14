package br.com.ezy.ezypaymentapplicationbackend.config.exception;

import java.time.Instant;
import java.util.List;

public record ApiResponseError(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<String> details
) {
}
