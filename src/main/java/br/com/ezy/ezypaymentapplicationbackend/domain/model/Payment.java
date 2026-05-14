package br.com.ezy.ezypaymentapplicationbackend.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payments")
public record Payment(
        @Id
        String id,
        String firstName,
        String lastName,
        String zipCode,
        String encryptedCardNumber
){}
