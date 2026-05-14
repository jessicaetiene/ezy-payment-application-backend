package br.com.ezy.ezypaymentapplicationbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableMongoAuditing
@SpringBootApplication
public class EzyPaymentApplicationBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EzyPaymentApplicationBackendApplication.class, args);
    }

}
