# Prompt Management Service

## Overview
The Prompt Management Service is a core component of the AgentFlow platform, responsible for prompt template creation, versioning, testing, and analytics.

## Features
- Prompt template management
- Versioning system
- Parameter framework
- LLM provider integrations
- Testing capabilities
- Analytics tracking

## Development Setup

### Prerequisites
- Java 17
- Maven
- Docker and Docker Compose
- MariaDB

### Local Development
1. Clone the repository
2. Navigate to the prompt-service directory
3. Run `docker-compose up -d` to start the database
4. Run `mvn spring-boot:run -Dspring-boot.run.profiles=dev` to start the application

### API Documentation
- Swagger UI: http://localhost:8082/api/prompt-service/swagger-ui.html
- OpenAPI JSON: http://localhost:8082/api/prompt-service/v3/api-docs

## Building and Testing
- Run `mvn clean package` to build the application
- Run `mvn test` to run tests