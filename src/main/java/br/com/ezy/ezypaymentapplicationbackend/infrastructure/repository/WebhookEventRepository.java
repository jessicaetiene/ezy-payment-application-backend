package br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository;

import br.com.ezy.ezypaymentapplicationbackend.domain.model.Webhook;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEvent;
import br.com.ezy.ezypaymentapplicationbackend.domain.model.WebhookEventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WebhookEventRepository extends MongoRepository<WebhookEvent, String> {
    List<WebhookEvent> findTop10ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(WebhookEventStatus status, Instant nextAttemptAt);
}
