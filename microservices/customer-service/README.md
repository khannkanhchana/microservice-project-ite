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
no longer exposed. Swagger UI is available at /swagger-ui.html.
