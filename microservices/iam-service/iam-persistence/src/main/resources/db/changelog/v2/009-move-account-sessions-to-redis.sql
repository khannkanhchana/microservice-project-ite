-- Active device sessions now live in Redis (AccountSessionRegistry): one hash per session with an idle TTL,
-- a per-user index set, and the OAuth2 authorizations bound to each session. Login history and account
-- activity stay in PostgreSQL.
DROP TABLE IF EXISTS account_session_authorizations;
DROP TABLE IF EXISTS account_sessions;
