# Ezy Payment Application Backend

A Spring Boot backend for creating payment records and notifying registered webhook subscribers when new payments are created.

The application exposes REST endpoints to:

- Register webhook URLs.
- Create payments.
- Encrypt and persist card numbers in MongoDB.
- Queue webhook delivery events for every registered webhook.
- Deliver webhook notifications asynchronously and retry failed deliveries with exponential backoff.

## Table of contents

- [Technology stack](#technology-stack)
- [What the code does](#what-the-code-does)
- [Project structure](#project-structure)
- [Requirements](#requirements)
- [Configuration](#configuration)
- [How to run locally](#how-to-run-locally)
- [API documentation](#api-documentation)
- [REST API usage](#rest-api-usage)
- [Database collections](#database-collections)
- [Webhook delivery behavior](#webhook-delivery-behavior)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## Technology stack

- Java 17
- Spring Boot 3.3.5
- Spring Web
- Spring Data MongoDB
- Spring Validation
- Spring Actuator
- Spring Scheduling and Async execution
- Springdoc OpenAPI / Swagger UI
- MongoDB 7 through Docker Compose
- Gradle Kotlin DSL

## What the code does

### Application bootstrap

### Payments flow

1. A client sends `POST /payments` with `firstName`, `lastName`, `zipCode`, and `cardNumber`.
2. `PaymentController` validates the request body and calls `CreatePaymentUseCase`.
3. `CreatePaymentUseCase` encrypts the card number with `CardEncryptorService`.
4. The encrypted card number is stored in the `payments` MongoDB collection through `PaymentRepository`.
5. After the payment is saved, `WebhookEventPublisher` creates one pending webhook event for each registered webhook.
6. Each webhook event is dispatched asynchronously by `WebhookDispatcher`.
7. The API response returns the payment id and non-sensitive customer fields. The encrypted card number is not returned.

### Card encryption

`CardEncryptorService` uses AES/GCM/NoPadding. It removes whitespace from the card number, generates a random 12-byte IV for every encryption, encrypts the normalized card number, prepends the IV to the encrypted bytes, and Base64-encodes the result before storage.

The encryption key is configured with `security.card-encryption-key` and must be a Base64-encoded AES key.

### Webhook registration flow

1. A client sends `POST /webhooks` with a valid HTTP or HTTPS URL.
2. `WebhookController` validates the request body and calls `CreateWebhookPaymentUseCase`.
3. `CreateWebhookPaymentUseCase` stores the webhook in the `webhooks` MongoDB collection through `WebhookRepository`.
4. The API response returns the webhook id and URL.

### Webhook delivery and retries

When a payment is created, `WebhookEventPublisher` reads all registered webhooks and creates pending events in the `webhook_events` collection.

`WebhookDispatcher` posts the saved payment payload to each webhook URL. If delivery succeeds, the event status becomes `DELIVERED`. If delivery fails, the dispatcher either:

- schedules the event for another attempt with exponential backoff; or
- marks it as `FAILED` when the configured maximum attempt count is reached.

`WebhookRetryScheduler` runs periodically and dispatches up to 10 pending events whose `nextAttemptAt` timestamp is due.

## Project structure

```text
.
├── build.gradle.kts                         # Gradle build, Java version, and dependencies
├── docker-compose.yaml                      # Local MongoDB service
├── openapi.json                             # Checked-in OpenAPI snapshot
├── settings.gradle.kts                      # Gradle root project name
├── src/main/java/br/com/ezy/ezypaymentapplicationbackend
│   ├── EzyPaymentApplicationBackendApplication.java
│   ├── api
│   │   ├── payment                         # Payment REST controller and DTOs
│   │   └── webhook                         # Webhook REST controller and DTOs
│   ├── application
│   │   ├── payment                         # Payment use case
│   │   └── webhook                         # Webhook use cases, publisher, dispatcher, scheduler
│   ├── config
│   │   ├── exception                       # Global API error handling
│   │   └── openapi                         # Swagger/OpenAPI configuration
│   ├── domain
│   │   ├── model                           # MongoDB document records and enum
│   │   └── service                         # Card encryption service
│   └── infrastructure
│       └── repository                      # Spring Data MongoDB repositories
├── src/main/resources/application.yml       # Runtime configuration
└── src/test/java                            # Spring Boot context test
```

## Requirements

Install the following before running the project:

- Java 17 or newer compatible with Java 17 toolchains.
- Docker and Docker Compose, for local MongoDB.
- Bash-compatible shell, macOS/Linux/WSL, or use `gradlew.bat` on Windows.

You do not need to install Gradle manually because the repository includes the Gradle wrapper.

## Configuration

Default configuration lives in `src/main/resources/application.yml`.

| Property | Default | Description |
| --- | --- | --- |
| `server.port` | `8080` | HTTP port for the Spring Boot API. |
| `spring.data.mongodb.uri` | `mongodb://root:root@localhost:27017/ezy-payment?authSource=admin` | MongoDB connection URI. |
| `security.card-encryption-key` | configured in `application.yml` | Base64 AES key used to encrypt card numbers. |
| `webhooks.max-attempts` | `5` | Maximum delivery attempts before a webhook event is marked `FAILED`. |
| `webhooks.retry-delay-ms` | `30000` | Fixed delay, in milliseconds, between retry scheduler executions. |
| `management.endpoints.web.exposure.include` | `health,info` | Actuator endpoints exposed over HTTP. |

## How to run locally

### 1. Clone the repository

```bash
git clone <repository-url>
cd ezy-payment-application-backend
```

If you are already inside this repository, stay at the repository root.

### 2. Start MongoDB

```bash
docker compose up -d
```

This starts MongoDB 7 on `localhost:27017` with username `root` and password `root`.

Check that the container is running:

```bash
docker compose ps
```

### 3. Start the application

On Linux, macOS, or WSL:

```bash
./gradlew bootRun
```

On Windows Command Prompt or PowerShell:

```powershell
.\gradlew.bat bootRun
```

The API should start on:

```text
http://localhost:8080
```


### 4. Stop local services

Stop the Spring Boot process with `Ctrl+C`, then stop MongoDB:

```bash
docker compose down
```

To also delete the MongoDB volume and all local data:

```bash
docker compose down -v
```

## API documentation

When the application is running, use:

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Actuator health: <http://localhost:8080/actuator/health>

The repository also includes an `openapi.json` snapshot.

## REST API usage

### Create a webhook

Registers a URL that will receive payment notifications.

```bash
curl -i -X POST http://localhost:8080/webhooks \
  -H 'Content-Type: application/json' \
  -d '{
    "url": "https://example.com/webhook"
  }'
```

Successful response status: `201 Created`.

Example response:

```json
{
  "id": "6a05fe1f4fb87134417de9b5",
  "url": "https://example.com/webhook"
}
```

Validation rules:

- `url` is required.
- `url` must start with `http://` or `https://`.

### Create a payment

Creates a payment and queues asynchronous webhook notifications for all registered webhooks.

```bash
curl -i -X POST http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Jessica",
    "lastName": "Almeida",
    "zipCode": "01023-999",
    "cardNumber": "4111 1111 1111 1111"
  }'
```

Successful response status: `201 Created`.

Example response:

```json
{
  "id": "6a0601867a3bf641f16744ec",
  "firstName": "Jessica",
  "lastName": "Almeida",
  "zipCode": "01023-999"
}
```

Validation rules:

- `firstName` is required and must not be blank.
- `lastName` is required and must not be blank.
- `zipCode` is required and must not be blank.
- `cardNumber` is required and must not be blank.

The card number is encrypted before it is stored and is not included in the API response.

### Example validation error

```json
{
  "timestamp": "2026-05-14T17:07:43.368175Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation Failed",
  "path": "/payments",
  "details": [
    "firstName: must not be blank"
  ]
}
```

## Database collections

The application stores documents in these MongoDB collections:

| Collection | Model | Purpose |
| --- | --- | --- |
| `payments` | `Payment` | Stores customer name, ZIP code, and encrypted card number. |
| `webhooks` | `Webhook` | Stores registered webhook URLs. |
| `webhook_events` | `WebhookEvent` | Stores delivery status, attempts, retry timestamp, payload, and last error for webhook notifications. |

## Webhook delivery behavior

Webhook notification payloads are the saved `Payment` object. That means the webhook receiver receives fields such as:

```json
{
  "id": "payment-id",
  "firstName": "Jessica",
  "lastName": "Almeida",
  "zipCode": "01023-999",
  "encryptedCardNumber": "base64-encrypted-value"
}
```

Delivery behavior:

- New webhook events start as `PENDING`.
- Successful HTTP delivery changes the status to `DELIVERED`.
- Failed delivery increments the attempt count.
- Failed events are retried with exponential backoff: 2 seconds after the first failed attempt, 4 seconds after the second, 8 seconds after the third, and so on.
- Events are marked `FAILED` after `webhooks.max-attempts` attempts.
- The scheduler scans due pending events every `webhooks.retry-delay-ms` milliseconds and dispatches up to 10 at a time.


