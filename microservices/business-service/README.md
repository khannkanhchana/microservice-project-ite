# Business Service OAuth2 resource server

Business Service runs on port 9092 and requires JWT bearer tokens for its REST APIs.
It uses a reactive `SecurityWebFilterChain` and `NimbusReactiveJwtDecoder` to verify
RS256 signatures with the authorization server's public JWK set. Validation also
checks the issuer, expiration and not-before timestamps, with Spring Security's
default 60-second clock skew. Authentication is stateless; CSRF, HTTP Basic and
form login are disabled.

| Method | Route | Required scope |
| --- | --- | --- |
| GET | `/api/businesses` and subpaths | `business.read` |
| POST | `/api/businesses` and subpaths | `business.write` |

All other routes are denied by default. Missing or invalid tokens return 401 with
a Bearer challenge. Valid tokens lacking the required scope return 403.

| Environment variable | Default |
| --- | --- |
| `AUTH_SERVER_ISSUER` | `http://localhost:9000` |
| `AUTH_SERVER_JWK_SET_URI` | `http://localhost:9000/oauth2/jwks` |

The issuer must match the token's `iss` claim exactly. The JWK Set URI must be
reachable from Business Service. Keys are fetched when needed and cached, so the
authorization server need not be running at Business Service startup.

The authorization server registers `business-client` / `business-secret` for
`business.read`, using `client_credentials` and `client_secret_basic`. Override
these defaults using `BUSINESS_OAUTH2_CLIENT_ID` and `BUSINESS_OAUTH2_CLIENT_SECRET`
in both Authorization Server and Order. Register a separate writer client with
`business.write` if creation is needed. The existing `customer-client` only grants
`customer.read` and cannot access Business APIs.

Request a token with the registered business scope, then send it to Business Service:

```powershell
curl.exe -u "<registered-client-id>:<registered-client-secret>" -X POST "http://localhost:9000/oauth2/token" -d "grant_type=client_credentials&scope=business.read"
curl.exe "http://localhost:9092/api/businesses" -H "Authorization: Bearer <access_token>"
```

Builds and tests were not run for this change, as requested.

See [OAuth2 validation](../../docs/oauth2-validation.md) for the checks you can run.
