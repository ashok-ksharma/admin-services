# Local JWT generator

Static RS256 tokens for the local WireMock IAM. There is no Keycloak in the local stack. The app validates tokens offline against the JWKS that WireMock serves.

## Run

From `local-dev-setup/`, JDK 21, no build step:

```bash
java tools/jwt/GenerateTokens.java              # reuse keys/jwt-signing-key.pem (created if missing)
java tools/jwt/GenerateTokens.java --new-key    # rotate the signing key first
```

`./run-local-stack.sh tokens` runs the first command and reloads WireMock.

| Input / output | Purpose |
|---|---|
| `tools/jwt/tokens.properties` | Token list: type, clientId, username, roles. Also issuer, kid and validity (100 years). |
| `keys/jwt-signing-key.pem` | RSA private key (PKCS#8). Created on the first run on each machine. For local use only; gitignored, never committed. |
| `deps/wiremock/mappings/keycloak-authmanager.json` | Generated WireMock stubs: the JWKS (public key) and the signed tokens. Committed. Do not edit by hand. Hand-written stubs go in other mapping files. |

## What is committed

Only `keycloak-authmanager.json` is committed; the signing key is not. The stack needs only the JSON: WireMock serves the public key and the tokens from it, so a fresh clone works without the key. The key is needed only to sign tokens again.

On a machine without the key, the first run creates a new key and re-signs every token, so the whole `keycloak-authmanager.json` changes, including the JWKS. That is expected.

## When to run `tokens`

| Situation | Run `tokens`? | Then |
|---|---|---|
| Fresh clone, `up`, `down`, `wipe`, IDE restart | No | The committed `keycloak-authmanager.json` already holds the tokens |
| You added or changed a user, client or role in `tokens.properties` | Yes | Restart admin-service if the run created a new key. Commit `tokens.properties` and `keycloak-authmanager.json`, never the key |
| You changed `issuer`, `kid` or `validity.years` in `tokens.properties` | Yes | As above |
| WireMock login returns 404 for a user or client | Only after adding it to `tokens.properties` | As above |
| You pulled a changed `keycloak-authmanager.json` | No | Reload WireMock (`curl -X POST http://localhost:8083/__admin/mappings/reset`) and restart admin-service, which keeps the old JWKS until restarted |
| You ran it without changing `tokens.properties` | (already done) | Nothing to commit: `git checkout -- deps/wiremock/mappings/keycloak-authmanager.json`, then reload WireMock |

## What the stubs serve

| Endpoint | Matched on | Returns |
|---|---|---|
| `GET /auth/realms/*/protocol/openid-connect/certs` | | JWKS (public key, `kid` from the properties) |
| `POST /v1/authmanager/authenticate/clientidsecretkey` | body contains the client id | client token (header, cookie, body) |
| `POST /auth/realms/*/protocol/openid-connect/token` | body contains the client id | client token (`access_token`) |
| `POST /auth/realms/*/protocol/openid-connect/token` | HTTP Basic `Authorization` header for the client (Keycloak admin client) | client token (`access_token`) |
| `POST /v1/authmanager/authenticate/internal/useridPwd`, `.../useridPwd` | `request.userName` | user token (header, cookie, body) |

An unknown user or client gets WireMock's 404, so a missing token shows up straight away.

## Add a role or user

1. Add a `token.<name>.*` block to `tokens.properties`.
2. Run `./run-local-stack.sh tokens`. It regenerates `keycloak-authmanager.json` and reloads WireMock.
3. Restart admin-service if the run created a new key.
4. Commit `tokens.properties` and `keycloak-authmanager.json`.

Rerunning re-signs every token with a new `iat`/`exp`. Old tokens stay valid while the signing key is unchanged. After a new key (`--new-key`, or the first run on a machine without the key), old tokens never validate again. New tokens validate only after admin-service is restarted, because it keeps the old JWKS (the `kid` does not change).

## Get a token

The tokens live only in the generated mapping. Get one from the running WireMock, the same way the API test rig does:

```bash
curl -s -X POST localhost:8083/v1/authmanager/authenticate/internal/useridPwd \
  -H 'Content-Type: application/json' -d '{"request":{"userName":"globaladmin"}}'
```

Send it as `Cookie: Authorization=<jwt>`.

## Expiry

The `exp` claim is set to `validity.years` (100). That claim is what the app checks. The `expires_in` / `expiryTime` fields in the stub responses are capped at 2147483647 s (about 68 years) so that clients reading them as an `int` don't overflow.

## Permission errors on the signing key

The generator writes `keys/jwt-signing-key.pem` on the host as your user. Errors come from the `keys/` folder or the file not being writable by you.

| Symptom | Cause | Steps to fix |
|---|---|---|
| `tokens` fails with `java.nio.file.AccessDeniedException: keys` or `keys/jwt-signing-key.pem` | Linux / macOS: `keys/` or the file belongs to another user (for example created with `sudo`). Windows: the file is read-only or open in another program | 1. Linux / macOS: `ls -ld keys keys/jwt-signing-key.pem` to see the owner.<br>2. Linux / macOS: `sudo chown "$(id -u):$(id -g)" keys` (and `keys/jwt-signing-key.pem` if it exists).<br>3. Windows: close any program that has the file open, then `attrib -r keys\jwt-signing-key.pem`.<br>4. Run `./run-local-stack.sh tokens` again |
| `rm keys/jwt-signing-key.pem`: `Permission denied` | Same as above: `keys/` is not writable by you | 1. Do not delete the key to rotate it; run `java tools/jwt/GenerateTokens.java --new-key`, which overwrites it.<br>2. If that also fails: fix the owner as in the row above, then retry |

The key is local only. Losing or deleting it breaks nothing: the next `tokens` run creates a new one (see "What is committed").

## Current tokens

| Name | Type | Used by |
|---|---|---|
| `admin-client` | client `mosip-admin-client` | the app's self-token for keymanager, audit, WebSub and other outbound calls |
| `masterdata-220005` | user | api-test role `admin` (`<module>-<admin_userName>`) |
| `globaladmin` | user | api-test role `globalAdmin` (`admin_zone_userName`) |
| `testrig-client` | client `mosip-testrig-client` | API test rig's Keycloak admin client (user search / create, stubbed in `api-test-rig.json`) |
