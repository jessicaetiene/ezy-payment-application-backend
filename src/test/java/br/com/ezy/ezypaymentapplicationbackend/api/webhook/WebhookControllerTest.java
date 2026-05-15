package br.com.ezy.ezypaymentapplicationbackend.api.webhook;

import br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository.WebhookRepository;
import br.com.ezy.ezypaymentapplicationbackend.support.MongoTestContainerSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WebhookControllerTest extends MongoTestContainerSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebhookRepository webhookRepository;

    @BeforeEach
    void setUp() {
        webhookRepository.deleteAll();
    }

    @Test
    void shouldCreateWebhook() throws Exception {
        var request = new WebhookRequest("https://merchant.test/webhook");

        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.url").value("https://merchant.test/webhook"));

        assertThat(webhookRepository.findAll())
                .singleElement()
                .satisfies(webhook -> assertThat(webhook.url()).isEqualTo("https://merchant.test/webhook"));
    }

    @Test
    void shouldReturnBadRequestWhenUrlIsBlank() throws Exception {
        var request = new WebhookRequest("");

        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.path").value("/webhooks"))
                .andExpect(jsonPath("$.details").isArray());

        assertThat(webhookRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenUrlDoesNotUseHttpOrHttps() throws Exception {
        var request = new WebhookRequest("ftp://merchant.test/webhook");

        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation Failed"))
                .andExpect(jsonPath("$.path").value("/webhooks"))
                .andExpect(jsonPath("$.details").value(org.hamcrest.Matchers.hasItem("url: must be a valid HTTP or HTTPS URL")));

        assertThat(webhookRepository.findAll()).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/webhooks"));

        assertThat(webhookRepository.findAll()).isEmpty();
    }
}
