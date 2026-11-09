# IAM Service

**Identity and Access Management Service**

A modular Identity and Access Management (IAM) system built with **Spring Boot, Spring Authorization Server, OAuth 2.1, and OpenID Connect 1.0**.

The system provides centralized authentication, authorization, user management, role-based access control (RBAC), permission management, OAuth2 client management, and federated social authentication.

---

## Table of Contents

- [1. Overview](#1-overview)
- [2. Features](#2-features)
- [3. Technology Stack](#3-technology-stack)
- [4. System Architecture](#4-system-architecture)
- [5. Project Structure](#5-project-structure)
- [6. Core Domain Model](#6-core-domain-model)
- [7. Authentication and Authorization](#7-authentication-and-authorization)
- [8. Social Login](#8-social-login)
- [9. Dynamic Client Registration](#9-dynamic-client-registration)
- [10. Internationalization](#10-internationalization)
- [11. Database Design](#11-database-design)
- [12. Prerequisites](#12-prerequisites)
- [13. Environment Configuration](#13-environment-configuration)
- [14. Installation and Setup](#14-installation-and-setup)
- [15. Running the Application](#15-running-the-application)
- [16. API Documentation](#16-api-documentation)
- [17. Testing](#17-testing)
- [18. Postman Collection](#18-postman-collection)
- [19. Security Best Practices](#19-security-best-practices)
- [20. Error Handling](#20-error-handling)
- [21. Contributing](#21-contributing)


---

## 1. Overview

IAM Service is a centralized identity and access management system designed to manage users, roles, permissions, identities, and OAuth2 clients.

It follows a modular architecture with Clean Architecture principles to separate business logic from infrastructure frameworks.

The core domain and application layers remain independent of Spring Authorization Server, Spring Data JPA, and web technologies.

Spring Authorization Server is integrated as an infrastructure adapter responsible for OAuth2 and OpenID Connect protocol operations.

The system supports both machine-to-machine authentication and user-based authentication.

### Main Objectives

- Provide centralized identity and access management.
- Implement secure authentication and authorization.
- Support OAuth 2.1 and OpenID Connect 1.0.
- Manage users, roles, and permissions.
- Support federated authentication through social identity providers.
- Provide a secured REST API.
- Support English and Khmer user interfaces.
- Maintain clean, maintainable, and testable code.

---

## 2. Features

### User Management

- User registration.
- Retrieve user information.
- Update user profiles.
- Soft deletion.
- Account status management.
- User identity management.
- Pagination and searching.

### Role Management

- Create and manage roles.
- Assign roles to users.
- Revoke roles from users.
- Retrieve roles assigned to users.
- Soft deletion and auditing.

### Permission Management

- Create and manage permissions.
- Assign permissions to roles.
- Assign direct permissions to users.
- Revoke permissions.
- Permission-based authorization.

### Authentication and Authorization

- OAuth 2.1.
- OpenID Connect 1.0.
- JWT access tokens.
- Refresh tokens.
- Authorization Code Flow with PKCE.
- Client Credentials Flow.
- Role-based access control (RBAC).
- Permission-based access control.
- JSON Web Key Set (JWKS).
- OIDC UserInfo endpoint.

### OAuth2 Client Management

- Registered OAuth2 clients.
- JDBC-based client persistence.
- Dynamic Client Registration (DCR).
- OAuth2 authorization persistence.
- OAuth2 authorization consent persistence.

### Social Authentication

- Google Login.
- GitHub Login.
- Facebook Login.
- Telegram Authentication.

### Web Interface

- Server-side rendering with JTE.
- English language (en).
- Khmer language (km).
- Login page.
- Registration page.
- Consent page.
- User profile page.
- OAuth2 error pages.

---

## 3. Technology Stack

| Category | Technology |
|---|---|
| Programming Language | Java |
| Framework | Spring Boot |
| Security | Spring Security |
| Authorization Server | Spring Authorization Server |
| Authentication Protocol | OAuth 2.1 |
| Identity Protocol | OpenID Connect 1.0 |
| Token Format | JWT |
| Persistence | Spring Data JPA |
| Database | PostgreSQL |
| Database Migration | Liquibase |
| Template Engine | JTE |
| Build Tool | Gradle |
| Unit Testing | JUnit 5 |
| Mocking | Mockito |
| Assertions | AssertJ |
| Integration Testing | Spring Boot Test |
| Database Testing | Testcontainers |
| API Testing | Postman |
| Version Control | Git |

---

## 4. System Architecture

The project follows Clean Architecture and a modular design.

The main architectural principle is to keep business rules independent of external frameworks and infrastructure.

### Architecture Overview

```text
                  Client Applications
                          |
                          v
                  Spring Security
                          |
                          v
              Spring Authorization Server
                          |
             +------------+------------+
             |                         |
             v                         v
        OAuth2 / OIDC              JWT Tokens
             |
             v
          iam-web
             |
             v
         iam-rest-api
             |
             v
    iam-application-service
             |
             v
       iam-domain-core
             ^
             |
       Output Ports
             |
             v
       iam-persistence
             |
             v
         PostgreSQL
```

### Architectural Principles

- Dependency Inversion Principle.
- Single Responsibility Principle.
- Separation of Concerns.
- Domain Independence.
- Dependency Injection.
- Ports and Adapters.
- One Use Case = One Class.
- Controllers only map DTOs and invoke use cases.

### Module Responsibilities

**iam-domain-core**

Contains the core business domain and business rules.

It must not depend on:

- Spring Authorization Server.
- Spring Data JPA.
- Spring MVC.
- Web frameworks.
- Infrastructure implementations.

**iam-application-service**

Contains application use cases and output ports.

Responsibilities:

- Coordinate business operations.
- Execute application workflows.
- Define repository and service ports.
- Validate business rules.
- Return application results.

Each use case is implemented in its own class.

**iam-persistence**

Implements application output ports using Spring Data JPA.

Responsibilities:

- JPA entities.
- Repository implementations.
- Entity-to-domain mapping.
- Database persistence.
- Auditing.
- Optimistic locking.

Each assignment model is mapped as its own entity instead of using `@ManyToMany`.

**iam-rest-api**

Provides REST API endpoints.

Responsibilities:

- REST controllers.
- Request and response DTOs.
- Request validation.
- API exception handling.
- RFC 9457 Problem Details.
- JWT-based API security.

Controllers should only map DTOs and call application use cases.

**iam-infrastructure**

Provides infrastructure integrations.

Responsibilities:

- Spring Authorization Server configuration.
- OAuth2 registered client persistence.
- OAuth2 authorization persistence.
- OAuth2 consent persistence.
- JWT customization.
- OIDC claim mapping.
- Dynamic Client Registration.
- Social authentication integration.

**iam-web**

Provides server-rendered web pages using JTE.

Responsibilities:

- Login page.
- Registration page.
- OAuth2 consent page.
- Error pages.
- Internationalization.
- English and Khmer translations.

---

## 5. Project Structure

```text
iam-service/
│
├── iam-domain-core/
│   └── src/
│       ├── main/java/
│       └── test/java/
│
├── iam-application-service/
│   └── src/
│       ├── main/java/
│       └── test/java/
│
├── iam-persistence/
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       │   └── db/changelog/
│       └── test/java/
│
├── iam-rest-api/
│   └── src/
│       ├── main/java/
│       └── test/java/
│
├── iam-infrastructure/
│   └── src/
│       ├── main/java/
│       └── test/java/
│
├── iam-web/
│   └── src/
│       ├── main/java/
│       ├── main/resources/
│       │   ├── templates/
│       │   └── messages/
│       └── test/java/
│
├── postman/
│   ├── IAM-Service.postman_collection.json
│   └── IAM-Local.postman_environment.json
│
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle
├── build.gradle
├── gradle.properties
├── .env.example
├── .gitignore
└── README.md
```

The package and directory structure may be extended as the project grows.

---

## 6. Core Domain Model

The system is built around five core domain concepts and three audited assignment models.

### User

Represents an individual account in the IAM system.

Responsibilities:

- Store user information.
- Manage account status.
- Update user details.
- Support soft deletion.

### Role

Represents a collection of permissions.

Examples:

- SYSTEM_ADMIN
- ADMIN
- USER
- MODERATOR

### Permission

Represents an individual authorization capability.

Examples:

- USER_READ
- USER_CREATE
- USER_UPDATE
- USER_DELETE
- ROLE_ASSIGN
- PERMISSION_MANAGE

### Identity

Represents an external or federated identity associated with a user.

Examples:

- Google identity.
- GitHub identity.
- Facebook identity.
- Telegram identity.

### Assignment Models

The system uses separate assignment models:

1. UserRole: Assigns roles to users.
2. RolePermission: Assigns permissions to roles.
3. UserPermission: Assigns permissions directly to users.

Each assignment model has its own persistence entity.

### Authorization Relationship

```text
             USER
               |
       +-------+-------+
       |               |
       v               v
    UserRole      UserPermission
       |               |
       v               v
      ROLE          PERMISSION
       |
       v
  RolePermission
       |
       v
   PERMISSION
```

Effective permissions can be calculated from both assigned roles and direct user permissions.

---

## 7. Authentication and Authorization

The system uses Spring Authorization Server to implement OAuth2 and OpenID Connect.

### Supported OAuth2 Flows

**Authorization Code Flow with PKCE**

Designed for user authentication through client applications.

Flow:

```text
Client Application
       |
       v
Authorization Request
       |
       v
IAM Login Page
       |
       v
User Authentication
       |
       v
Authorization Consent
       |
       v
Authorization Code
       |
       v
Token Endpoint
       |
       v
Access Token + ID Token
```

**Client Credentials Flow**

Designed for machine-to-machine communication.

Flow:

```text
Client Application
       |
       v
Client ID + Client Secret
       |
       v
Token Endpoint
       |
       v
Access Token
       |
       v
Protected Resource API
```

**Refresh Token**

Allows supported clients to obtain a new access token without repeating the full authentication process.

### JWT Access Token Claims

The access token may contain:

```json
{
  "iss": "http://localhost:9000",
  "sub": "user-uuid",
  "aud": ["iam-api"],
  "roles": [
    "ADMIN",
    "USER"
  ],
  "permissions": [
    "USER_READ",
    "USER_CREATE"
  ]
}
```

The actual claim structure depends on the configured JWT token customizer.

### Authorization

The REST API validates JWT access tokens and uses role and permission authorities to control access to protected resources.

---

## 8. Social Login

The IAM system supports federated authentication through external identity providers.

Supported providers:

- Google
- GitHub
- Facebook
- Telegram

### Federated Authentication Flow

```text
User
 |
 v
Select Social Provider
 |
 v
External Authentication
 |
 v
Receive Provider Identity
 |
 v
FindOrProvisionFederatedUserUseCase
 |
 +---------------------+
 |                     |
 v                     v
Existing User       New User
 |                     |
 +----------+----------+
            |
            v
       IAM Account
            |
            v
      Authorization
```

The application uses `FindOrProvisionFederatedUserUseCase` to locate or provision a local user based on the authenticated external identity.

External provider credentials must be configured through environment variables.

---

## 9. Dynamic Client Registration

Dynamic Client Registration (DCR) allows OAuth2 clients to register with the Authorization Server through an API instead of requiring manual registration.

### Main Responsibilities

- Accept client registration requests.
- Validate registration metadata.
- Register OAuth2 clients.
- Persist registered client information.
- Return client registration information according to the implemented registration protocol.

### Example Registration Request

```http
POST /connect/register
Content-Type: application/json
```

Example metadata:

```json
{
  "client_name": "My Application",
  "redirect_uris": [
    "http://localhost:3000/callback"
  ],
  "grant_types": [
    "authorization_code",
    "refresh_token"
  ],
  "response_types": [
    "code"
  ],
  "token_endpoint_auth_method": "none"
}
```

This example assumes a public PKCE client. The accepted fields and response depend on the actual DCR implementation.

---

## 10. Internationalization

The web interface supports two languages:

| Language | Code |
|---|---|
| English | en |
| Khmer | km |

JTE is used for server-side HTML rendering.

Translation messages are maintained separately for each supported language.

Example:

```text
messages/
├── messages_en.properties
└── messages_km.properties
```

Example translation keys:

```properties
# English
login.title=Welcome Back
login.email=Email Address
login.password=Password
login.submit=Sign In
```

```properties
# Khmer
login.title=សូមស្វាគមន៍ការត្រឡប់មកវិញ
login.email=អាសយដ្ឋានអ៊ីមែល
login.password=ពាក្យសម្ងាត់
login.submit=ចូលគណនី
```

Users can switch between English and Khmer through the language selector.

---

## 11. Database Design

PostgreSQL is used as the primary relational database.

Liquibase manages database schema migrations.

### Main Domain Tables

The database includes tables for:

- users
- roles
- permissions
- identities
- user_roles
- role_permissions
- user_permissions

Additional OAuth2 tables are used for registered clients, authorizations, and authorization consents.

### Database Best Practices

**Soft Delete**

Records are logically deleted instead of being physically removed.

**Auditing**

Entities maintain audit information such as:

- created_at
- updated_at
- created_by
- updated_by
- deleted_at

**Optimistic Locking**

Version fields are used to prevent conflicting concurrent updates.

**Database Migration**

Liquibase changelogs are used for all database schema changes.

Manual schema modifications should not replace version-controlled migrations.

---

## 12. Prerequisites

Before running the application, make sure the following tools are installed:

| Tool | Requirement |
|---|---|
| JDK | Project-compatible Java version |
| Gradle | Gradle Wrapper included |
| PostgreSQL | Supported database version |
| Docker | Required for container-based testing |
| IntelliJ IDEA | Recommended IDE |
| Postman | API testing |

Verify Java:

```bash
java -version
```

Verify Git:

```bash
git --version
```

Use the Gradle Wrapper included in the repository instead of relying on a globally installed Gradle version.

---

## 13. Environment Configuration

Sensitive information must never be hardcoded into the source code.

Configure the following environment variables according to the application's configuration.

### Database

```properties
DB_URL=
DB_USERNAME=
DB_PASSWORD=
```

### JWT and Authorization Server

```properties
JWT_ISSUER=http://localhost:9000
```

### Google OAuth2

```properties
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
```

### GitHub OAuth2

```properties
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
```

### Facebook OAuth2

```properties
FACEBOOK_CLIENT_ID=
FACEBOOK_CLIENT_SECRET=
```

### Telegram

```properties
TELEGRAM_BOT_TOKEN=
```

### Important

The variable names above are examples. Use the exact variable names referenced by your Spring configuration.

Create a local `.env` file or configure environment variables through IntelliJ IDEA Run Configurations.

Never commit actual credentials or private keys to Git.

The `.env.example` file should contain placeholders only.

---

## 14. Installation and Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/khannkanhchana/microservice-project-ite.git
```

### Step 2: Navigate to the Project

```bash
cd iam-service
```

### Step 3: Configure Environment Variables

Configure database credentials, JWT issuer, and social provider credentials.

### Step 4: Create the PostgreSQL Database

Example:

```sql
CREATE DATABASE iam_db;
```

Make sure the database connection configuration matches your environment variables.

### Step 5: Run Database Migrations

Liquibase migrations should execute through the configured application startup or migration task.

### Step 6: Build the Project

Windows:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```bash
./gradlew clean build
```

---

## 15. Running the Application

Run the configured Spring Boot application using the Gradle Wrapper.

Windows:

```powershell
.\gradlew.bat bootRun
```

Linux/macOS:

```bash
./gradlew bootRun
```

The Authorization Server issuer is configured as:

```text
http://localhost:9000
```

The actual application port and available endpoints depend on the active Spring Boot configuration.

### Common OAuth2 Endpoints

When configured by Spring Authorization Server, the standard endpoints include:

| Endpoint | Purpose |
|---|---|
| `/oauth2/authorize` | Authorization endpoint |
| `/oauth2/token` | Token endpoint |
| `/oauth2/jwks` | JSON Web Key Set |
| `/oauth2/revoke` | Token revocation |
| `/oauth2/introspect` | Token introspection |
| `/.well-known/openid-configuration` | OIDC Discovery |
| `/userinfo` | OIDC UserInfo |

Dynamic Client Registration is exposed only when the DCR endpoint is configured and enabled.

---

## 16. API Documentation

The REST API exposes application use cases through dedicated controllers and DTOs.

### API Categories

**User Management**

- Register User.
- Retrieve User.
- Retrieve Users.
- Update User.
- Soft Delete User.

**Role Management**

- Create Role.
- Retrieve Roles.
- Update Role.
- Assign Role.
- Revoke Role.

**Permission Management**

- Create Permission.
- Retrieve Permissions.
- Assign Permission.
- Revoke Permission.

**Identity Management**

- Link external identity.
- Retrieve linked identities.
- Manage federated identities.

### Security

Protected endpoints require a valid JWT access token.

Use the following HTTP header:

```http
Authorization: Bearer <ACCESS_TOKEN>
```

The API uses permission authorities to enforce access control.

For the exact endpoint paths, request payloads, and response formats, refer to the implemented REST controllers and the Postman collection.

---

## 17. Testing

The project uses unit testing and integration testing to verify business logic, persistence, security, and OAuth2 functionality.

### Unit Testing

Unit tests verify individual classes independently.

Technologies:

- JUnit 5
- Mockito
- AssertJ

Main testing areas:

- Domain business rules.
- Application use cases.
- Input validation.
- Exception handling.
- Role and permission assignments.
- Federated user provisioning.

### Integration Testing

Integration tests verify that multiple components work together correctly.

Technologies:

- Spring Boot Test
- Testcontainers
- PostgreSQL
- Spring Security Test
- MockMvc

Main testing areas:

- JPA repository operations.
- PostgreSQL persistence.
- Liquibase migrations.
- Auditing.
- Optimistic locking.
- REST API security.
- JWT authentication.
- Permission authorization.
- OAuth2 token generation.
- Authorization Code with PKCE.
- OIDC and JWKS.
- Dynamic Client Registration.

### Run Unit and Integration Tests

Windows:

```powershell
.\gradlew.bat clean test
```

Linux/macOS:

```bash
./gradlew clean test
```

### Build and Verify All Modules

```bash
./gradlew clean build
```

Windows:

```powershell
.\gradlew.bat clean build
```

### Test Reports

Gradle generates HTML test reports inside:

```text
<module>/build/reports/tests/test/index.html
```

Open the report to review passed, failed, and skipped tests.

Integration tests should use an isolated test database, preferably through Testcontainers.

---

## 18. Postman Collection

A Postman collection is provided to test the IAM REST API and OAuth2 endpoints.

### Collection Structure

```text
IAM Service API
│
├── 01 - Authentication
│   ├── Register User
│   ├── OAuth2 Token
│   ├── Refresh Token
│   ├── JWKS
│   └── OIDC UserInfo
│
├── 02 - Users
│   ├── Create User
│   ├── Get All Users
│   ├── Get User By ID
│   ├── Update User
│   └── Delete User
│
├── 03 - Roles
│   ├── Create Role
│   ├── Get All Roles
│   ├── Update Role
│   ├── Assign Role
│   └── Revoke Role
│
├── 04 - Permissions
│   ├── Create Permission
│   ├── Get All Permissions
│   ├── Assign Permission
│   └── Revoke Permission
│
└── 05 - OAuth2 Clients
    └── Dynamic Client Registration
```

### Postman Environment

Create an environment named `IAM Local`.

| Variable | Description |
|---|---|
| baseUrl | Authorization Server base URL |
| apiUrl | REST API base URL |
| clientId | OAuth2 client ID |
| clientSecret | OAuth2 client secret |
| accessToken | JWT access token |
| refreshToken | Refresh token |
| userId | User identifier |

### Import Collection

1. Open Postman.
2. Select Import.
3. Import `IAM-Service.postman_collection.json`.
4. Import `IAM-Local.postman_environment.json`.
5. Select the `IAM Local` environment.
6. Configure the required variables.
7. Execute the API requests.

Never include real secrets or access tokens in exported Postman files.

---

## 19. Security Best Practices

The project follows important security practices:

- Keep secrets in environment variables.
- Never commit passwords, tokens, or private keys.
- Use secure password hashing.
- Validate incoming request data.
- Verify JWT signatures.
- Validate token issuer, audience, and expiration.
- Apply permission-based authorization.
- Use PKCE for public OAuth2 clients.
- Use HTTPS in production.
- Apply soft deletion to required domain entities.
- Maintain auditing information.
- Use optimistic locking to prevent concurrent update conflicts.
- Avoid exposing sensitive user information in API responses.
- Validate external identity provider information before provisioning accounts.

---

## 20. Error Handling

The REST API follows RFC 9457 Problem Details for HTTP APIs.

Example error response:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "User not found",
  "instance": "/api/users/123"
}
```

Common HTTP status codes:

| Status | Description |
|---|---|
| 200 | OK |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict |
| 500 | Internal Server Error |

Error responses should provide useful information without exposing internal implementation details or sensitive data.

---

## 21. Contributing

Contributions and improvements are welcome.

### Development Guidelines

1. Follow the existing module architecture.
2. Keep domain and application modules framework-independent.
3. Implement one use case per class.
4. Keep controllers focused on HTTP mapping and use case invocation.
5. Create a separate persistence entity for each assignment model.
6. Use Liquibase for database migrations.
7. Add unit tests for new business logic.
8. Add integration tests for infrastructure and API changes.
9. Never commit sensitive environment variables.
10. Update documentation when adding new features.

### Git Workflow

```bash
git checkout -b feature/your-feature-name
```

Commit your changes:

```bash
git add .
git commit -m "feat: implement your feature"
```

Push your branch:

```bash
git push -u origin feature/your-feature-name
```


---


