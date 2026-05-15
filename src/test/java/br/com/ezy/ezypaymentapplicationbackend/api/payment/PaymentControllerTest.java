package br.com.ezy.ezypaymentapplicationbackend.api.payment;

import br.com.ezy.ezypaymentapplicationbackend.application.webhook.WebhookEventPublisher;
import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.PaymentRepository;
import br.com.ezy.ezypaymentapplicationbackend.support.MongoTestContainerSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@SpringBootTest
@AutoConfigureMockMvc
public class PaymentControllerTest extends MongoTestContainerSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private WebhookEventPublisher webhookEventPublisher;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void shouldCreatePaymentWithoutReturningEncryptedCardNumber() throws Exception {
        var request = new PaymentRequest("Jessica", "Almeida", "01023-999", "4111 1111 1111 1111");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Jessica"))
                .andExpect(jsonPath("$.lastName").value("Almeida"))
                .andExpect(jsonPath("$.zipCode").value("01023-999"))
                .andExpect(jsonPath("$.encryptedCardNumber").doesNotExist())
                .andExpect(jsonPath("$.cardNumber").doesNotExist());

        var savedPayments = paymentRepository.findAll();
        assertThat(savedPayments).hasSize(1);
        assertThat(savedPayments.getFirst().encryptedCardNumber()).isNotBlank();
        assertThat(savedPayments.getFirst().encryptedCardNumber()).doesNotContain("4111");
        verify(webhookEventPublisher).execute(savedPayments.getFirst());
    }

    @Test
    void shouldStillCreatePaymentWhenWebhookPublishingFails() throws Exception {
        doThrow(new RuntimeException("publisher unavailable")).when(webhookEventPublisher).execute(any());
        var request = new PaymentRequest("Jessica", "Almeida", "01023-999", "4111111111111111");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(paymentRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldReturnBadRequestWhenRequiredFieldsAreBlank() throws Exception {
        var request = new PaymentRequest("", "", "", "");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.path").value("/payments"))
                .andExpect(jsonPath("$.details").isArray());

        assertThat(paymentRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/payments"));

        assertThat(paymentRepository.findAll()).isEmpty();
    }
}
