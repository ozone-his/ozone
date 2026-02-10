# EIP HCW@Home OpenMRS RabbitMQ Service

This is a RabbitMQ-enabled EIP service for HCW@Home ↔ OpenMRS integration, built with Spring Boot and Apache Camel.

## Features
- Debezium-based monitoring of OpenMRS database for patient and encounter changes.
- RabbitMQ producer for emitting events to HCW@Home.
- RabbitMQ consumer for receiving events from HCW@Home and posting to OpenMRS via REST API.

## Architecture
This service follows the peer-to-peer EIP architecture of Ozone HIS.

## Configuration
Configuration is managed via `application.properties` and environment variables.
