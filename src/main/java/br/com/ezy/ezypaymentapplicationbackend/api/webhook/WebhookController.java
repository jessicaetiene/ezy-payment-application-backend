package br.com.ezy.ezypaymentapplicationbackend.api.webhook;

import br.com.ezy.ezypaymentapplicationbackend.api.payment.PaymentRequest;
import br.com.ezy.ezypaymentapplicationbackend.api.payment.PaymentResponse;
import br.com.ezy.ezypaymentapplicationbackend.application.payment.CreatePaymentUseCase;
import br.com.ezy.ezypaymentapplicationbackend.application.webhook.CreateWebhookPaymentUseCase;
import br.com.ezy.ezypaymentapplicationbackend.config.exception.ApiResponseError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Tag(
        name = "Webhooks",
        description = "Endpoint to create a new payment"
)
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final CreateWebhookPaymentUseCase createWebhookPaymentUseCase;

    public WebhookController(CreateWebhookPaymentUseCase createWebhookPaymentUseCase) {
        this.createWebhookPaymentUseCase = createWebhookPaymentUseCase;
    }

    @Operation(
            summary = "Create a webhook",
            description = "Creates a new webhook."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Webhook created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "url": "https://webhook",
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseError.class),
                            examples = @ExampleObject(value = """
                                    {
                                    	"timestamp": "2026-05-14T17:14:53.485420Z",
                                    	"status": 400,
                                    	"error": "Bad Request",
                                    	"message": "Validation Failed",
                                    	"path": "/webhooks",
                                    	"details": [
                                    		"url: must be a valid HTTP or HTTPS URL"
                                    	]
                                    }
                                    """)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<WebhookResponse> create(@Valid @RequestBody WebhookRequest webhookRequest){
        var webhook = this.createWebhookPaymentUseCase.execute(webhookRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new WebhookResponse(webhook.id(), webhook.url()));
    }
}
