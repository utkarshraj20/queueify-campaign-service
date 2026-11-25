# Queueify Campaign Service

Queueify Campaign Service is responsible for managing users, sender email accounts, campaigns, and campaign execution workflows for the Queueify platform.

## Features

* User authentication using JWT
* Sender email account management
* Campaign creation and management
* CSV upload support
* Campaign execution
* Kafka integration for email processing
* Retry mechanism and failure handling
* Campaign progress tracking

## Tech Stack

* Java 21
* Spring Boot 3
* Spring Security
* Spring Data JPA
* MySQL
* Flyway
* Docker
* Apache Kafka
* Maven

## Prerequisites

* Java 21
* Docker
* Maven

## Running Locally

### Start MySQL

```bash
docker compose up -d
```

### Run the application

```bash
mvn spring-boot:run
```

The service starts on:

```
http://localhost:8080
```

## Database Migration

Flyway is used for database schema management.

Migration files are located under:

```
src/main/resources/db/migration
```

## Project Structure

```
src/main/java/com/queueify/campaignservice

config/
controller/
dto/
entity/
enums/
exception/
repository/
security/
service/
util/
```

## Future Enhancements

* Multiple sender email accounts
* Campaign processor
* Kafka producer integration
* Retry mechanism
* Dashboard APIs
* Observability and metrics
* Pause and resume campaigns

## License

This project is for learning and portfolio purposes.


## Internal campaign delivery context (KAN-68)

Disabled by default. CAMPAIGN_CONTEXT_API_ENABLED=true enables the read-only endpoint and
requires QUEUEIFY_INTERNAL_CAMPAIGN_TOKEN (a random 32–256 character URL-safe service token).
Use the same campaign token in Email Service. It is distinct from user JWTs, AWS keys and the
Processor task-lookup token. Do not expose it in frontend code, Kafka, URLs or committed files.
Remote deployments require TLS termination and network restrictions.

GET /internal/v1/campaigns/{campaignId}/delivery-context requires the campaign service bearer token.
A dedicated stateless Spring Security chain handles /internal/**: user JWTs are not accepted,
only this GET route is authorized, and the shared token grants no access to normal user APIs.
Internal routes remain denied when the flag is off. Existing user JWT security is retained.

The response includes campaignId, userId, subject, templateUri and senderEmailAccountId only,
with Cache-Control: no-store. The campaign must be RUNNING, its sender ACTIVE and owned by the
same user, and its template reference must use the configured S3 bucket and owner prefix.
Unknown campaigns return 404; ineligible campaigns return 409; invalid IDs return 400.

This is a point-in-time metadata read, not a delivery claim. No sender credential is returned,
no status/counter changes occur, and no template file is downloaded by this endpoint.
Email Service loads the referenced file separately. Provider credential access, durable delivery
ownership/results, rendering and sending remain subsequent increments.
