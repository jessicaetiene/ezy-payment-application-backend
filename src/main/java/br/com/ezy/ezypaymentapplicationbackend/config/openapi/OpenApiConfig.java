package br.com.ezy.ezypaymentapplicationbackend.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI configureCustomAPI() {
        return new OpenAPI().info(
                new Info().title("Payment Application")
                        .version("1.0.0")
                        .description("API documentation for the Catalog Pricing Service")
                        .contact(new Contact().name("Jessica Etiene Marques Almeida"))
        );
    }
}
