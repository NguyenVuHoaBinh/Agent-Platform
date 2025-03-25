# Identity & Access Management (IAM) Service

The Identity & Access Management Service is a foundational component of the AgentFlow platform, providing secure authentication, authorization, and organizational management capabilities.

## Features

- User authentication and authorization
- Multi-factor authentication
- Role-based access control
- Organization and project management
- Team and user management
- OAuth 2.1 and OpenID Connect support
- Audit logging for security and compliance

## Technology Stack

- Java 17
- Spring Boot 3.x
- Spring Security 6.x
- MariaDB
- Redis
- JWT (JSON Web Tokens)
- Flyway for database migrations

## Development Setup

### Prerequisites

- JDK 17
- Maven
- Docker and Docker Compose
- Git

### Local Development

1. Clone the repository:
   ```
   git clone https://github.com/your-org/agentflow.git
   cd agentflow/services/identity-service
   ```

2. Build the project:
   ```
   mvn clean install
   ```

3. Start the development environment:
   ```
   docker-compose up -d
   ```

4. Run the application:
   ```
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

The service will be available at http://localhost:8080/api

### API Documentation

When the service is running, you can access the API documentation at:

- Swagger UI: http://localhost:8080/api/swagger-ui
- OpenAPI Spec: http://localhost:8080/api/api-docs

## Project Structure

```
src/main/java/viettel/dac/identityservice/
├── IdentityServiceApplication.java
├── common/
│   ├── constants/
│   ├── exception/
│   └── util/
├── config/
├── controller/
├── dto/
├── entity/
├── repository/
├── security/
└── service/
```

## Database Schema

The IAM service uses a comprehensive database schema that includes:

- User accounts and profiles
- Organizations and projects
- Roles and permissions
- Teams and memberships
- Audit logs and security records

Database migrations are managed through Flyway and can be found in `src/main/resources/db/migration`.

## Testing

Run tests with:

```
mvn test
```

For integration tests:

```
mvn verify
```

## Security

The service implements industry-standard security practices:

- Secure password storage with bcrypt
- JWT-based authentication
- Role-based access control
- Multi-factor authentication
- Audit logging

## Deployment

The service can be deployed using Docker:

```
docker build -t identity-service .
docker run -p 8080:8080 identity-service
```

For production deployment, use Kubernetes with the provided configuration files in the `k8s` directory.

## Contributing

1. Follow the coding standards in the documentation
2. Write tests for new features
3. Update documentation for significant changes
4. Submit a pull request with a clear description of the changes

## License

[License Information]