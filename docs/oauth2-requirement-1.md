# Requirement 1: Order calls a protected Customer API

## Service responsibilities

| Service | OAuth2 role | Responsibility |
| --- | --- | --- |
| Authorization Server | Authorization server | Registers the Order client, authenticates its credentials, and issues signed access tokens. |
| Order | Confidential OAuth2 client | Obtains an access token using `client_credentials` and attaches it to outgoing Customer requests. |
| Customer | Resource server | Validates the JWT signature, issuer, validity period, and required scope before allowing access. |

Order requests a token; Authorization Server issues it. This machine-to-machine
flow has no human username/password login, authorization code, redirect URI, or
refresh token. The client ID and client secret identify Order's registered client.

## Registered client

Authorization Server initializes the client in `RegisteredClientRepository` at
startup. The existing registration named `order-client` is Order's client for
calling Customer; the name does not make Customer an OAuth2 client.

| Setting | Local default |
| --- | --- |
| Client ID | `order-client` |
| Client secret | `order-secret` |
| Grant type | `client_credentials` |
| Client authentication | `client_secret_basic` |
| Allowed scope | `customer.read` |
| Token endpoint | `http://localhost:9000/oauth2/token` |

Set `OAUTH2_CLIENT_ID` and `OAUTH2_CLIENT_SECRET` consistently in Authorization
Server and Order to override the defaults. Authorization Server stores an encoded
secret. Registrations and signing keys are currently in memory.

## Request flow

1. A caller sends `GET /customers` to Order. This route is public under the current requirement.
2. Order's OAuth2 WebClient filter checks its token cache for the `customer-service` registration.
3. If needed, Order sends `POST /oauth2/token` to Authorization Server, with HTTP Basic client authentication and form fields `grant_type=client_credentials` and `scope=customer.read`.
4. Authorization Server authenticates the registered client and returns a signed JWT access token.
5. Order sends `GET /api/customers` to Customer with `Authorization: Bearer <access_token>`.
6. Customer obtains public signing keys from `/oauth2/jwks` and validates the token locally. It checks the issuer, signature, timestamps, and `customer.read` scope. Public keys are cached; validation does not require a token-endpoint call on every request.
7. Customer returns its data and Order returns the response to the caller.

Order reuses valid cached tokens and obtains another token near expiry. Failed
token acquisition never falls back to an unauthenticated Customer call. Token
endpoint network failures and the 10-second token request timeout become a safe
503 response. Valid cached tokens can continue to work during an authorization
server outage.

## Customer access rules

| Request | Required authorization |
| --- | --- |
| `GET /actuator/health` | Public |
| `GET /api/customers` and child paths | Valid JWT with `customer.read` |
| `POST /api/customers` and child paths | Valid JWT with `customer.write` |
| Other paths | Denied |

The Order client is read-only: it cannot obtain `customer.write` or create a
customer. Missing or invalid tokens receive 401; valid tokens with insufficient
scope receive 403.

## Configuration and manual use

Customer uses `AUTH_SERVER_ISSUER` (default `http://localhost:9000`) and
`AUTH_SERVER_JWK_SET_URI` (default `http://localhost:9000/oauth2/jwks`). Order uses
`AUTH_SERVER_TOKEN_URI` (default `http://localhost:9000/oauth2/token`) and
`CUSTOMER_SERVICE_URL` (default `http://localhost:9091`). URLs must be reachable
from the service using them; set appropriate hostnames when running in containers.

Start infrastructure and Authorization Server, Customer, and Order. Restart Order
after changing credentials or restarting the in-memory Authorization Server to
clear cached tokens signed by the previous key. Reload Gradle after dependency
changes. Call Order's `/customers` on its configured port; no manually supplied
token is needed for that request.

To request a token manually in Postman, use POST, Basic Auth with the client ID
and secret above, and an `x-www-form-urlencoded` body containing the grant type
and scope. Use the returned access token as Bearer Auth when calling Customer
directly. A browser GET to `/oauth2/token` does not issue a token.
