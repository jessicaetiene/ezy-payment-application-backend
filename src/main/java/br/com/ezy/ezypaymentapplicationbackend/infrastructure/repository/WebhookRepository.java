package br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.Webhook;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookRepository extends MongoRepository<Webhook, String> {
}
