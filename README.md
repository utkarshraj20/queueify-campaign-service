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
