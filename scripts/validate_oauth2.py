"""Opt-in checks against running services. Uses only Python's standard library."""
import argparse
import base64
import json
import os
import time
import unittest
import urllib.error
import urllib.parse
import urllib.request


AUTH = os.getenv("AUTH_SERVER_URL", "http://localhost:9000").rstrip("/")
CUSTOMER = os.getenv("CUSTOMER_SERVICE_URL", "http://localhost:9091").rstrip("/")
BUSINESS = os.getenv("BUSINESS_SERVICE_URL", "http://localhost:9092").rstrip("/")
ORDER = os.getenv("ORDER_SERVICE_URL", "http://localhost:8080").rstrip("/")


def request(url, method="GET", token=None, credentials=None, form=None):
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    if credentials:
        basic = base64.b64encode(":".join(credentials).encode()).decode()
        headers["Authorization"] = "Basic " + basic
    data = None
    if form is not None:
        data = urllib.parse.urlencode(form).encode()
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            return response.status, response.read(), response.headers
    except urllib.error.HTTPError as error:
        return error.code, error.read(), error.headers


def credentials(service):
    if service == "business":
        return (os.getenv("BUSINESS_OAUTH2_CLIENT_ID", "business-client"),
                os.getenv("BUSINESS_OAUTH2_CLIENT_SECRET", "business-secret"))
    return (os.getenv("OAUTH2_CLIENT_ID", "customer-client"),
            os.getenv("OAUTH2_CLIENT_SECRET", "customer-secret"))


def obtain_token(service):
    status, body, _ = request(AUTH + "/oauth2/token", method="POST",
                              credentials=credentials(service),
                              form={"grant_type": "client_credentials", "scope": service + ".read"})
    if status != 200:
        raise AssertionError("Token acquisition failed for " + service + ": HTTP " + str(status))
    return json.loads(body)["access_token"]


class LiveOAuth2Checks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tokens = {service: obtain_token(service) for service in ("customer", "business")}

    def test_valid_credentials_and_read_access(self):
        for service, base, route in (("customer", CUSTOMER, "/api/customers"),
                                      ("business", BUSINESS, "/api/businesses")):
            with self.subTest(service=service):
                status, body, _ = request(base + route, token=self.tokens[service])
                self.assertEqual(status, 200)
                self.assertIsInstance(json.loads(body), list)

    def test_invalid_client_credentials(self):
        for service in ("customer", "business"):
            for client, secret in ((credentials(service)[0], "intentionally-wrong-secret"),
                                   ("unregistered-validation-client", "invalid-secret")):
                with self.subTest(service=service, unknown_client=client.startswith("unregistered")):
                    status, body, _ = request(AUTH + "/oauth2/token", method="POST",
                                              credentials=(client, secret),
                                              form={"grant_type": "client_credentials"})
                    self.assertEqual(status, 401)
                    self.assertEqual(json.loads(body)["error"], "invalid_client")

    def test_missing_malformed_and_tampered_tokens_return_401(self):
        for service, base, route in (("customer", CUSTOMER, "/api/customers"),
                                      ("business", BUSINESS, "/api/businesses")):
            original = self.tokens[service].split(".")
            signature = original[2]
            # Change a significant base64url signature character, preserving JWT syntax.
            original[2] = ("A" if signature[0] != "A" else "B") + signature[1:]
            for token in (None, "not-a-jwt", ".".join(original)):
                with self.subTest(service=service, case="missing" if token is None else "invalid"):
                    status, _, headers = request(base + route, token=token)
                    self.assertEqual(status, 401)
                    self.assertTrue(headers.get("WWW-Authenticate", "").startswith("Bearer"))

    def test_cross_service_scopes_return_403(self):
        for url, token in ((CUSTOMER + "/api/customers", self.tokens["business"]),
                           (BUSINESS + "/api/businesses", self.tokens["customer"])):
            with self.subTest(url=url):
                self.assertEqual(request(url, token=token)[0], 403)

    def test_read_tokens_cannot_write(self):
        for service, url in (("customer", CUSTOMER + "/api/customers"),
                              ("business", BUSINESS + "/api/businesses")):
            with self.subTest(service=service):
                self.assertEqual(request(url, method="POST", token=self.tokens[service])[0], 403)

    def test_order_calls_both_services_without_an_incoming_bearer_token(self):
        # Both downstream APIs require JWTs: success exercises automatic acquisition
        # and propagation by Order's actual configured HTTP service groups.
        for path in ("/customers", "/businesses"):
            with self.subTest(path=path):
                status, body, _ = request(ORDER + path)
                self.assertEqual(status, 200)
                self.assertIsInstance(json.loads(body), list)


class AuthorizationOutageChecks(unittest.TestCase):
    def test_authorization_is_unavailable_and_cold_order_returns_503(self):
        try:
            status, _, _ = request(AUTH + "/oauth2/token", method="POST",
                                    credentials=credentials("customer"),
                                    form={"grant_type": "client_credentials"})
            self.assertGreaterEqual(status, 500, "Stop Authorization Server before outage mode")
        except urllib.error.URLError:
            pass
        for path in ("/customers", "/businesses"):
            with self.subTest(path=path):
                status, body, _ = request(ORDER + path)
                self.assertEqual(status, 503, "Restart Order after stopping Authorization Server to clear cached tokens")
                self.assertEqual(json.loads(body)["title"], "Service authorization unavailable")


class ExpirationChecks(unittest.TestCase):
    def test_signed_expired_tokens_rejected_and_order_renews(self):
        tokens = {service: obtain_token(service) for service in ("customer", "business")}
        expirations = []
        for token in tokens.values():
            payload = token.split(".")[1]
            claims = json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
            expirations.append(claims["exp"])
        wait = max(expirations) - time.time() + 65
        self.assertLessEqual(wait, 120, "Set OAUTH2_ACCESS_TOKEN_TTL=2s and restart Authorization Server first")
        for path in ("/customers", "/businesses"):
            self.assertEqual(request(ORDER + path)[0], 200)
        # Capture and retain the original signed tokens; never edit their claims.
        # Recompute after warm-up so the sleep doesn't add to request latency.
        time.sleep(max(0, max(expirations) - time.time() + 65))
        for service, url in (("customer", CUSTOMER + "/api/customers"),
                              ("business", BUSINESS + "/api/businesses")):
            self.assertEqual(request(url, token=tokens[service])[0], 401)
        for path in ("/customers", "/businesses"):
            self.assertEqual(request(ORDER + path)[0], 200)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--outage", action="store_true", help="Run with Authorization Server stopped and Order freshly restarted")
    modes.add_argument("--expiration", action="store_true", help="Requires Authorization Server TTL=2s; waits through clock skew")
    args = parser.parse_args()
    case = AuthorizationOutageChecks if args.outage else ExpirationChecks if args.expiration else LiveOAuth2Checks
    result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(case))
    raise SystemExit(0 if result.wasSuccessful() else 1)
