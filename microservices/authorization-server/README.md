# OAuth2 Authorization Server

Standalone Spring Security Authorization Server for machine-to-machine access.
Run from the repository root with JDK 25:

```powershell
.\gradlew.bat -p microservices/authorization-server bootRun
```

The token endpoint accepts `POST /oauth2/token` with form-encoded
`grant_type=client_credentials` and `scope=customer.read`. Authenticate using
HTTP Basic (`client_secret_basic`). Successful responses contain a signed RS256
JWT, `token_type`, `expires_in`, and `scope`; no refresh token is issued.
Invalid credentials return 401, and invalid scopes or unsupported grants return 400.

```powershell
curl.exe -u "order-client:order-secret" -X POST "http://localhost:9000/oauth2/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials&scope=customer.read"
```

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `AUTH_SERVER_PORT` | `9000` | Listening port |
| `AUTH_SERVER_ISSUER` | `http://localhost:9000` | JWT issuer; set to the externally reachable URL |
| `OAUTH2_CLIENT_ID` | `order-client` | Registered client ID |
| `OAUTH2_CLIENT_SECRET` | `order-secret` | Client secret; override outside local development |
| `OAUTH2_ACCESS_TOKEN_TTL` | `15m` | Positive access token lifetime, e.g. `30m` or `1h` |
| `BUSINESS_OAUTH2_CLIENT_ID` | `business-client` | Separate client for Business reads |
| `BUSINESS_OAUTH2_CLIENT_SECRET` | `business-secret` | Business client secret |

The separate Business client is restricted to `business.read`; the Customer client
remains restricted to `customer.read`. Neither registration grants write access.

The registered client is held in `InMemoryRegisteredClientRepository`; its secret
is encoded with BCrypt. Add or update registrations through this repository in
`SecurityConfig`. There is no public client administration endpoint.

Public signing keys are available at `/oauth2/jwks` and server metadata at
`/.well-known/oauth-authorization-server`. An RSA-2048 key is generated at startup.
Keys and registrations are in memory: restarting replaces the signing key, so
previously issued tokens cannot be verified with the new key. Production deployments
need durable client storage, persistent signing keys shared across replicas, and HTTPS.

Run integration tests:

```powershell
.\gradlew.bat -p microservices/authorization-server test
```

Tests exercise the security filter chain, verify JWT signatures against the public
JWK, check issuer/scope/lifetime, and reject invalid authentication, scope and grant.
