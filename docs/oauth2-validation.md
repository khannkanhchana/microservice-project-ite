# OAuth2 validation

The checks below are provided for you to run. No builds, tests, service startup,
or validation scripts were executed for this change.

## Automated component and HTTP integration checks

From the repository root with JDK 25:

```powershell
.\gradlew.bat :microservices:authorization-server:test :microservices:customer-service:customer-service-main:test :microservices:business-service:business-service-main:test :microservices:order-service:order-service-client:test
.\gradlew.bat :microservices:order-service:order-service-main:test --tests '*OrderAuthorizationFailureTests'
```

| Coverage | Checks |
| --- | --- |
| Authorization Server | Valid client credentials; wrong secret; unknown client; missing auth; unsupported grant/authentication method; unregistered scope; signed JWT, issuer, scope and lifetime; separate business registration |
| Customer and Business resource servers | Real production security configuration and controllers with mocked use cases; public-key retrieval over local HTTP; valid signature/read scope; missing/malformed/expired/future-dated/wrong-issuer/wrong-signature JWTs; insufficient scope; read tokens denied POST |
| Order HTTP clients | Production client manager and OAuth2 filters; simulated HTTP token/resource servers; client credentials and scope request; Bearer header propagation; token reuse; separate registrations; renewal within expiry window; token-endpoint failure; cached token behavior during outage |

HTTP simulation is local to the JUnit tests. It does not claim that real services,
database connectivity, Consul configuration or deployed routing work. Resource
server tests validate actual signed JWTs; Order's simulated resource server only
checks propagated bearer headers. Postman CLI simulation remains unavailable due
to the previously observed managed manifest error.

## Live end-to-end checks

Start Authorization Server, Customer, Business and Order, together with their
configured databases and infrastructure. Run from the repository root with Python 3:

```powershell
$env:ORDER_SERVICE_URL = 'http://localhost:8080' # set the actual Order port
python scripts/validate_oauth2.py
```

The script checks both APIs directly, then calls Order's `/customers` and
`/businesses` without supplying an inbound token. Since both downstream services
require tokens, successful Order responses exercise automatic acquisition and
Bearer propagation through the real HTTP service groups. The script checks
response shapes but does not prove domain data correctness with empty databases.

Optional environment variables: `AUTH_SERVER_URL`, `CUSTOMER_SERVICE_URL`,
`BUSINESS_SERVICE_URL`, `ORDER_SERVICE_URL`, `OAUTH2_CLIENT_ID`,
`OAUTH2_CLIENT_SECRET`, `BUSINESS_OAUTH2_CLIENT_ID`, `BUSINESS_OAUTH2_CLIENT_SECRET`.
Use the same credentials in Authorization Server, Order and the script.
The script never prints tokens or secrets and performs no domain writes.

The business registration defaults to `business-client` / `business-secret`,
with `business.read`. Customer credentials still default to `customer-client` /
`customer-secret`, with `customer.read`. Neither client can create records.
Order's business URL defaults to `http://localhost:9092` and can be overridden by
`BUSINESS_SERVICE_URL`.

## Actual signed-token expiration and renewal

Set `OAUTH2_ACCESS_TOKEN_TTL=2s` for Authorization Server, restart it, and restart
Customer, Business and Order to clear cached keys and tokens. Run:

```powershell
python scripts/validate_oauth2.py --expiration
```

This separate mode retains the original signed tokens, warms both Order clients,
waits past expiration plus the resource servers' 60-second clock skew, checks that
both APIs reject the old tokens with 401, and checks Order succeeds by obtaining
new tokens. Expect roughly 67 seconds. The script refuses long token lifetimes
in this mode. Restore the normal TTL and restart services afterward.

## Authorization Server outage

Stop Authorization Server yourself and restart Order with its normal token URI,
leaving Customer and Business running. Run:

```powershell
python scripts/validate_oauth2.py --outage
```

The script confirms token acquisition is unavailable and both Order endpoints
return 503 with a safe problem response. Restarting Order makes its token cache
empty: valid cached tokens can legitimately continue working during an outage.
The HTTP integration tests cover that cached case separately. Token failures
never cause an unauthenticated fallback or automatic replay of a downstream write.
Restore Authorization Server after this check.

For network-failure testing, a token URI pointing to a closed local port can be
set through `AUTH_SERVER_TOKEN_URI` when launching a separate Order instance.
The script does not stop services or modify deployment configuration for you.
