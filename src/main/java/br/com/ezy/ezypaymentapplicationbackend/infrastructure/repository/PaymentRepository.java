package br.com.ezy.ezypaymentapplicationbackend.infrastructure.repository;


import br.com.ezy.ezypaymentapplicationbackend.domain.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
}
