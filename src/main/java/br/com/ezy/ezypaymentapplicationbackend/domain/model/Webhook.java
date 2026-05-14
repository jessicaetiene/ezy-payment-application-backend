package br.com.ezy.ezypaymentapplicationbackend.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "webhooks")
public record Webhook(
        @Id
        String id,
        String url
) { }
