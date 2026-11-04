# Order Service OAuth2 client

`CustomerHttpClient` is registered as a WebClient HTTP service proxy. Its
`customer-service` group uses `ServerOAuth2AuthorizedClientExchangeFilterFunction`
to obtain and attach `Authorization: Bearer <token>` automatically.

The client registration uses `client_credentials`, `client_secret_basic`, and
`customer.read`, matching the existing authorization server registration.

| Environment variable | Default |
| --- | --- |
| `OAUTH2_CLIENT_ID` | `order-client` |
| `OAUTH2_CLIENT_SECRET` | `order-secret` |
| `AUTH_SERVER_TOKEN_URI` | `http://localhost:9000/oauth2/token` |
| `CUSTOMER_SERVICE_URL` | `http://localhost:9091` |
| `BUSINESS_SERVICE_URL` | `http://localhost:9092` |
| `BUSINESS_OAUTH2_CLIENT_ID` | `business-client` |
| `BUSINESS_OAUTH2_CLIENT_SECRET` | `business-secret` |

Override the credentials outside local development and configure service URLs for
the deployment. The customer URL is a directly reachable HTTP URL.

`AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager` stores authorized
clients in memory, reuses valid tokens, and requests a new token on the next call
when the cached token is expired or within the default 60-second expiry window.
Client credentials renews access by requesting another token; it uses no refresh
token. The manager also works for calls outside an incoming web request.
An invalid-token response evicts the cached authorized client so a later request
can obtain a new token. Failed requests are not automatically replayed.

Each HTTP service group has its own OAuth2 filter and registration. The business
registration requests `business.read`; its `BusinessHttpClient` returns business
data through Order's `/businesses` endpoint. `/customers` calls Customer Service.
Token acquisition failures return 503 with a safe problem response. Cached valid
tokens can continue working while Authorization Server is unavailable.

Order Service retains its existing public inbound endpoints. Customer reads are allowed;
customer creation requires `customer.write`, which this read-only registration
does not have. The existing `getCustomerById` method targets a route that Customer
Service currently does not expose.

Builds and tests were not run for this change, as requested.

See [OAuth2 validation](../../docs/oauth2-validation.md) for component, HTTP
simulation, live end-to-end, expiration and outage checks you can run.
