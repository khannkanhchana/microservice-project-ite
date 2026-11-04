# Customer service

Customer service uses Spring WebFlux and PostgreSQL R2DBC throughout the HTTP,
application, and persistence layers. Start PostgreSQL before running the service.
The existing customers table is retained; an empty database gets the table from
`db/customer-schema.sql`. Configure the connection with `CUSTOMER_DB_URL`
(an R2DBC URL), `CUSTOMER_DB_USERNAME`, and `CUSTOMER_DB_PASSWORD`.

The controller exposes only these routes:

| Method | Route | Behavior |
| --- | --- | --- |
| GET | /api/customers | All customers without pagination |
| POST | /api/customers | Create a customer; returns 201 |
| GET | /api/customers/query | Exact-match filters: username, email, phoneNumber |

Both GET routes return a Flux of customers as a flat JSON array without pagination.
Query filters are optional, combined with AND; blank filters are ignored.
For example: `GET /api/customers/query?username=chana`.

POST JSON contains username, familyName, givenName, email, and phoneNumber.
All five fields are required and nonblank. PATCH/update/deactivate routes are
no longer exposed.

## OAuth2 resource server

Customer APIs require an `Authorization: Bearer <access_token>` header. GET routes
require `customer.read`; POST requires `customer.write`. The authorization server's
current `order-client` registration has only `customer.read`, so it cannot create
customers. Register an explicitly authorized writer client with `customer.write`
before using POST. Other routes, including Swagger UI, are denied by default.
`/actuator/health` and its subpaths remain public for Consul health checks.

The reactive security filter chain verifies RS256 JWT signatures using the public
JWK set, validates the issuer, expiration and not-before timestamps (with Spring
Security's default 60-second clock skew), and maps scopes to `SCOPE_` authorities.
Missing, malformed, expired, incorrectly signed or wrong-issuer tokens return 401
with a Bearer challenge. Valid tokens without the required scope return 403.
The API is stateless and uses bearer authentication; CSRF protection is disabled.

| Environment variable | Default |
| --- | --- |
| `AUTH_SERVER_ISSUER` | `http://localhost:9000` |
| `AUTH_SERVER_JWK_SET_URI` | `http://localhost:9000/oauth2/jwks` |

Set both URLs for the deployment. The issuer must exactly match the token's `iss`
claim; the JWK URL must be reachable from Customer Service. Keys are fetched when
needed and cached; the authorization server need not be running at service startup.

After starting the authorization server and Customer Service (with PostgreSQL):

```powershell
curl.exe -u "order-client:order-secret" -X POST "http://localhost:9000/oauth2/token" -d "grant_type=client_credentials&scope=customer.read"
curl.exe "http://localhost:9091/api/customers" -H "Authorization: Bearer <access_token>"
```

Run security and controller tests from the repository root:

```powershell
.\gradlew.bat :microservices:customer-service:customer-service-main:test :microservices:customer-service:customer-service-restapi:test
```
