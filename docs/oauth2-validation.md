# Manual OAuth2 testing in Postman

Start Authorization Server, Customer and Order with their databases and infrastructure.
Use the actual Order port shown in its startup log; Consul may configure it.

## 1. Obtain a token

- Method: POST
- URL: http://localhost:9000/oauth2/token
- Authorization: Basic Auth
- Username: order-client
- Password: order-secret
- Body: x-www-form-urlencoded
  - grant_type: client_credentials
  - scope: customer.read

Expect 200 OK with access_token. Copy that token for the next request.
Use your configured credentials if environment variables override these defaults.
Authorization Server issues the token; Order requests it as a service client.
A browser GET to the token endpoint does not issue a token.

## 2. Call Customer directly

GET http://localhost:9091/api/customers

In Authorization, select Bearer Token and paste the access_token.
Expect 200 OK and a JSON array. An empty array is valid with no customer data.
Repeat with No Auth: expect 401. Repeat with an invalid token: expect 401.

## 3. Call Customer through Order

GET http://localhost:9094/customers

Replace 9094 with the actual running Order port. Select No Auth.
Expect 200 OK. Order automatically obtains and sends its own access token to Customer.
Repeat to exercise caching. The response alone does not prove token reuse;
inspect Authorization Server request logs to count token requests.

## 4. Rejected credentials and permissions

| Request | Expected result |
| --- | --- |
| Token request with wrong Basic Auth password | 401, invalid_client |
| Token request with unregistered client ID | 401, invalid_client |
| Order client requests scope customer.write | 400, invalid_scope |
| POST /api/customers on Customer with read token and no body | 403; rejected before creation |

## Optional Business flow

Start Business. Request a token with Basic Auth business-client / business-secret,
using grant_type=client_credentials and scope=business.read.
GET http://localhost:9092/api/businesses with that Bearer token should return 200.
Without a token, expect 401. Using the Business token on Customer should return 403;
using the Customer token on Business should return 403.
GET Order's /businesses with No Auth should return 200.

## Optional expiration check

Set OAUTH2_ACCESS_TOKEN_TTL=2s in Authorization Server's launch environment.
Restart Authorization Server, Customer and Order to clear cached keys and tokens.
Obtain and retain a new token, then call Order's /customers once.
Wait until expiration plus at least 60 seconds for JWT clock skew.
Customer should reject the old token with 401; Order's /customers with No Auth
should return 200 by obtaining a new token. Restore the normal TTL afterward
and restart services.

## Optional Authorization Server outage check

Keep Customer running. Stop Authorization Server and restart Order to clear its
cached tokens. GET Order's /customers with No Auth should return 503 with title
Service authorization unavailable. Without restarting Order, cached valid tokens
may continue working. Restore Authorization Server and restart Order afterward.

Restart Authorization Server and Order after changing client credentials.
Restart Order after restarting the in-memory Authorization Server to clear tokens
signed with the previous key.