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

### Sender credential encryption primitive (KAN-68)

`CredentialCipher` provides a versioned `qcred:v1:` AES-256-GCM envelope using a
fresh random 12-byte nonce per encryption and a 128-bit authentication tag.
The owner user ID and credential purpose are authenticated alongside the data;
moving an envelope to a different user fails decryption. This does not bind an
envelope to an individual sender account within that user.

The constructor requires a Base64-encoded 32-byte key supplied externally. No
key is embedded, generated at startup, or configured by default. Password input
is limited to 256 UTF-8 bytes so envelopes fit the existing 512-character column.
Unknown versions, plaintext values, wrong keys and tampered envelopes fail closed.
Errors omit credential values and underlying cryptographic exception details.

This increment is a tested primitive only: account create/update still store the
supplied value unchanged. No existing database values are migrated and no
credential endpoint is exposed. Wiring encryption into writes, configuring key
lifecycle and explicitly replacing or migrating existing credentials are required
before claiming encrypted credential storage. Retain the encryption key securely;
losing it prevents decryption. Java Strings cannot be reliably erased from memory.
