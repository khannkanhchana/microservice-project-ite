# iam-web

React (Vite) frontend for the ISTAD Identity and Access Management service (`microservices/iam-service`).
English and Khmer (ខ្មែរ) UI, light and dark mode, and a responsive layout that works on phones and desktops.

## Run

The SPA is built into iam-service and served by it on port 9000 — no separate frontend server.

```bash
# 1. Start Postgres and Redis (sessions)
docker compose -f deployment/postgres/docker-compose.yaml up -d
docker compose -f deployment/redis/docker-compose.yaml up -d
# 2. Build + start iam-service (Gradle runs `npm install` + `npm run build` for this app)
cd microservices/iam-service
./gradlew :iam-main:bootRun
# 3. Open http://localhost:9000/login  → after signing in you land on http://localhost:9000/account
```

| URL | What |
|---|---|
| `http://localhost:9000/login` | The one sign-in page (JTE, authorization server) |
| `http://localhost:9000/` | React app home |
| `http://localhost:9000/account/...` | Profile, Security, Linked accounts, Sessions, Login history, Activity, Deactivate |

`npm run build` writes `index.html` + `assets/` to `iam-web/src/main/resources/static`, and `SpaController`
forwards the React routes to that `index.html`. Skip the frontend build with `-PskipFrontend`.

**UI development with hot reload (optional):** `npm run dev` serves the app at `http://localhost:5173`,
calling iam-service on :9000 (`IAM_WEB_DEV_URL` registers it as a redirect URI).

On startup, iam-service registers the public OAuth2 client `iam-web` (authorization code + PKCE, no secret,
redirect `http://localhost:9000/callback`). Change it with `IAM_WEB_BASE_URL` and `IAM_WEB_ALLOWED_ORIGINS`.

## How sign-in works

The SPA never handles passwords. **Sign in** redirects the browser to the IAM authorization server
(`/oauth2/authorize`, PKCE S256), where the user signs in with a password or a social provider, and then
exchanges the returned code at `/oauth2/token`. Access tokens are kept in `sessionStorage`. When a token
expires, the app re-authorizes, which completes without a prompt while the IAM browser session is alive.

## Account features

| Page | Route | API |
|---|---|---|
| Profile: name, username, email, phone, picture | `/account/profile` | `GET/PATCH /api/me`, `POST/DELETE /api/me/picture` |
| Security & password: change, or reset by email | `/account/security`, `/forgot-password`, `/reset-password` | `PUT /api/me/password`, `POST /api/auth/password/{forgot,reset}` |
| Linked accounts: Google, GitHub, Facebook, Telegram | `/account/linked-accounts` | `GET /api/me/identities`, `POST /api/me/identities/link`, `DELETE /api/me/identities/{id}` |
| Active sessions: device, browser, IP, location, last activity | `/account/sessions` | `GET /api/me/sessions` |
| Session management: end one device, others, or everywhere | `/account/sessions` | `DELETE /api/me/sessions/{id\|current}`, `DELETE /api/me/sessions?scope=others\|all` |
| Login history: successful and failed attempts | `/account/login-history` | `GET /api/me/login-history?status=all\|success\|failed` |
| Account activity | `/account/activity` | `GET /api/me/activity` |
| Account deactivation | `/account/deactivate` | `POST /api/me/deactivation` |

Notes:
- **Sessions are stored in Redis.** The IAM browser login (HTTP session) is kept by Spring Session under
  `iam:http-session:*`, and every active device session is a Redis hash `iam:account-session:{id}` (device,
  browser, OS, IP, location, created / last activity) indexed per user in `iam:user-sessions:{userUuid}`.
  Entries expire after `IAM_SESSION_TIMEOUT` (default 12h) without activity. Ending a session deletes its
  keys, its Spring Session and its OAuth2 authorizations.
- **Sessions.** A session is one browser sign-in to IAM. Every token issued from it carries an `iam_sid`
  claim. Ending a session immediately rejects its tokens, deletes its refresh tokens/authorizations, and
  forces that browser to sign in again.
- **Password reset email.** No mail server is configured, so the reset link is written to the iam-service
  log (`Password reset link for ...`).
- **Locations.** Private IPs show as "Local network". Set `IAM_GEOIP_ENABLED=true` to look up public IPs via
  ip-api.com (this sends client IPs to a third party).
- **Deactivation.** Takes effect immediately: the account is disabled and every device is signed out. An
  administrator re-enables it.
