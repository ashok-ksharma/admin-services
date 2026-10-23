# Local development setup for admin-service

Run, debug and test admin-service on your own machine, without a MOSIP cluster.

- **admin-service** runs on the host, from your IDE or as a jar: set breakpoints and restart it in seconds.
- **Its dependencies** run in Docker and start with one command.
- **No Kubernetes or Keycloak** needed.

## What you get

| | Details |
|---|---|
| **Real services** | PostgreSQL (`mosip_master`, `mosip_keymgr`), config server, keymanager |
| **Mocked services** | WireMock stands in for the IAM (Keycloak / authmanager) and the other MOSIP services admin-service calls |
| **Ready-to-use data** | A fresh database built from the repo's `db_scripts`, filled with sample test data; tokens for test users and clients already generated |
| **Built-in checks** | `smoke` checks the whole stack; the masterdata API test rig runs against it through WireMock |

## Where to start

| Step | Where to look |
|---|---|
| 1. Install the tools | "Prerequisites" |
| 2. Start the dependencies | "Run", step 1 |
| 3. Start admin-service | "Run", step 2 |
| 4. Check the stack | "Verify the stack" |
| 5. Call the APIs | "Authentication" |
| 6. Run the API test rig | "API test rig" |
| 7. Change config, data, stubs or tokens | `README.md` in that folder |
| 8. Stop | "Run", step 5 |

Something fails: "Troubleshooting".

## What runs where

admin-service runs on your machine. Everything it calls runs in Docker.

```text
  YOUR MACHINE                          DOCKER  (deps/docker-compose.yml)
 ┌───────────────────────┐             ┌──────────────────────────────────────┐
 │                       │── config ──►│  config-server   :51200              │
 │  admin-service :8086  │── data ────►│  postgres        :5456               │
 │  (IDE or jar)         │── login ───►│  wiremock        :8083  (mocks)      │
 │                       │── keys ────►│  keymanager      :8089               │
 └───────────────────────┘             │                                      │
                                       │  run once at start:                  │
                                       │    keymanager-keys-init              │
                                       │    masterdata-loader                 │
                                       └──────────────────────────────────────┘
```

| Component | Host port | Real / mock | Notes |
|---|---|---|---|
| admin-service | 8086 | real | runs on the host, not in Docker |
| PostgreSQL | 5456 | real | `postgres` / `mosip123`; `masteruser`, `keymgruser` / `mosip123` |
| Config server | 51200 | real | native mode over `deps/mosip-config` (port 51000 inside the container) |
| WireMock | 8083 | mock | IAM, auditmanager, authmanager userdetails, WebSub hub, regproc, idrepo, digitalcard |
| Keymanager | 8089 | real | no token check; keys created on first start |
| keymanager-keys-init | – | – | runs `chown` on the keystore volume before keymanager starts, then exits |
| masterdata-loader | – | real | `mosipid/masterdata-loader:1.3.1`, runs once and exits |

## Prerequisites

- Docker Desktop (Compose v2)
- JDK 21 and Maven 3.9 (for the IDE, `build`, `run`, `tokens`)
- curl

## Run

1. Start the dependencies:
   ```bash
   ./run-local-stack.sh up        # Windows: run-local-stack.bat up
   ```
   The first `up` creates `mosip_master` from `../../db_scripts`, loads the sample test data and creates the keymanager keys. Later runs reuse them.
2. Start admin-service, from the IDE or as a jar.

   **From the IDE.** Create a run configuration (IntelliJ: Run → Edit Configurations → Add → Application; other IDEs: a Java application run configuration):

   | Field | Value |
   |---|---|
   | JDK | 21 |
   | Module / classpath | `admin-service` |
   | Main class | `io.mosip.mergedadmin.AdminBootApplication` |
   | VM options | see below |
   | Program arguments, environment variables | none |

   ```text
   --add-modules=ALL-SYSTEM --add-opens=java.base/java.lang=ALL-UNNAMED
   -Dspring.profiles.active=default,local
   -Dspring.cloud.config.uri=http://localhost:51200/config
   ```
   Run or debug it. It is ready when the log shows `Started AdminBootApplication`, on port 8086.

   **As a jar.**
   1. Build the jar (from `admin/`, tests skipped):
      ```bash
      ./run-local-stack.sh build        # ends with BUILD SUCCESS
      ```
   2. Start it:
      ```bash
      ./run-local-stack.sh run          # port 8086; run 8096 for a second instance next to the IDE
      ```
      It uses the same VM options as the IDE. It is ready when the log shows `Started AdminBootApplication`.
   3. Leave the terminal open: the app runs in the foreground. Press Ctrl+C to stop it.
   4. After a code change: Ctrl+C, then `build` and `run` again. `run` starts the last built jar; it does not rebuild.
3. Check everything:
   ```bash
   ./run-local-stack.sh smoke
   ```
   Every line should be `[ OK ]`.
4. After a change, restart admin-service, then run `smoke` again. The containers keep running.
   - Overlay edit (`deps/mosip-config/admin-local.properties`): IDE: stop and run again. Jar: Ctrl+C, then `run`.
   - Code change: IDE: stop and run again (the IDE recompiles). Jar: Ctrl+C, `build`, then `run`.
5. To stop, stop admin-service, then `./run-local-stack.sh down` (the database and keys are kept), or leave the containers running for next time.

### Running a second instance

You can run a second admin-service next to the one in the IDE, for example to test a built jar while you keep debugging in the IDE. Both use the same containers and the same database; only the port differs.

| Instance | Port | Start | Check |
|---|---|---|---|
| IDE | 8086 | IDE run configuration | `./run-local-stack.sh smoke` |
| Second (jar) | 8096 | `./run-local-stack.sh build`, then `./run-local-stack.sh run 8096` | `./run-local-stack.sh smoke 8096` |

The API test rig always tests 8086. To test the second instance with the rig, see `deps/wiremock/README.md` ("Changing the admin-service port for the API test rig").

## Where the local configuration lives

| What | Where | Notes |
|---|---|---|
| admin-service config | `deps/mosip-config/admin-local.properties` | **All local changes go here**: localhost URLs and ports, DB, and the values a cluster injects from secrets |
| Keymanager config | `deps/mosip-config/keymanager-local.properties` | PKCS12 keystore, DB, placeholder salts, Spring Security off |
| Shared config | `deps/mosip-config/*-default.properties`, `identity-mapping.json`, `applicanttype.mvel` | Verbatim copies from the `mosip-config` repo. Do not edit them (see `deps/mosip-config/README.md`) |
| Containers, ports, images | `deps/docker-compose.yml` | |
| Database setup | `deps/init.sql`, `deps/keymanager/db_scripts/mosip_keymgr/` | Run once, on an empty volume. The keymanager scripts are a verbatim copy (see `deps/keymanager/README.md`) |
| Sample test data | `deps/mosip-data/xlsx/` | Verbatim copy from the `mosip-data` repo (see `deps/mosip-data/README.md`) |
| Mocked services | `deps/wiremock/mappings/admin-services.json` | Hand-written stubs (see `deps/wiremock/README.md`) |
| API test rig gateway | `deps/wiremock/mappings/api-test-rig.json` | Hand-written stubs (see `deps/wiremock/README.md`) |
| Users, clients, roles | `tools/jwt/tokens.properties` | Source for the generated `deps/wiremock/mappings/keycloak-authmanager.json` (see `tools/jwt/README.md`) |
| JWT signing key | `keys/jwt-signing-key.pem` | Created by the first `tokens` run, gitignored |
| Database and keystore | Docker volumes `admin-local_postgres-data` and `admin-local_keymanager-keys` | Not in the source tree. Kept by `up`, `down` and restarts; removed together only by `wipe` |

Each folder under `deps/` and `tools/jwt/` has a `README.md` with what it contains, its rules, and how to refresh it from its source: `deps/mosip-config`, `deps/mosip-data`, `deps/keymanager`, `deps/wiremock`, `tools/jwt`.

### How admin-service gets its configuration

admin-service gets its configuration from the config server, which serves the files in `deps/mosip-config`. When the same key is in more than one file, the file higher in this list wins:

1. `admin-local.properties`: local values; **put your changes here**. Loaded only with the `local` profile (`-Dspring.profiles.active=default,local`)
2. `admin-default.properties`
3. `kernel-default.properties`
4. `application-default.properties`

**Do not use `-D` options to change a value.** Values from the config server win over `-D` options, so a `-D` option has no effect when a file above sets the same key. Only these `-D` options work, because no served file sets them:

| `-D` option | Purpose |
|---|---|
| `-Dspring.profiles.active=default,local` | Turns on the `local` profile, so `admin-local.properties` is loaded |
| `-Dspring.cloud.config.uri=http://localhost:51200/config` | Where the config server is |
| `-Dserver.port=8096` | A different port, for example for a second instance |

**After you edit a file:** restart admin-service. The config server needs no restart; it reads the files again on every request. Details and checks: `deps/mosip-config/README.md`.

## Script commands

macOS / Linux / Git Bash: `./run-local-stack.sh <command>`. Windows cmd or PowerShell: `run-local-stack.bat <command>`. Both scripts have the same commands; the Windows one uses the `curl` built into Windows 10 and later.

| Command | What it does |
|---|---|
| `up` (default) | Starts postgres, config-server, WireMock and keymanager and waits until all four are healthy. If `master.zone` is empty (a fresh database), it runs `masterdata-loader` to load the sample test data. Prints the IDE VM options |
| `down` | Stops and removes the containers. The database and keystore volumes are kept, so the next `up` continues with the same data and keys |
| `wipe` | Removes the containers and both volumes (database and keystore) in one step, then runs `up`. You get a fresh database, new keys and the sample test data loaded again |
| `status` | Shows container status and health |
| `logs [svc]` | Follows one container's logs. The default is `keymanager`; others are `postgres`, `config-server`, `wiremock` |
| `tokens` | Regenerates the WireMock IAM tokens from `tools/jwt/tokens.properties` and reloads WireMock. Needed only after changing `tokens.properties` (see the "Authentication" section) |
| `build` | `mvn -pl admin-service -am package -DskipTests -Dgpg.skip=true` |
| `run [port]` | Runs the built jar against the stack with profiles `default,local`. The default port is 8086. Fails if the config server is not up |
| `smoke [port]` | Checks the dependencies and the app (default 8086); see the "Verify the stack" section |
| `help` | Lists the commands |

## Verify the stack

`./run-local-stack.sh smoke` prints one `[ OK ]` or `[FAIL]` line per check. It exits non-zero if any check fails.

| Check | Expected | What a pass proves |
|---|---|---|
| config-server `admin/default,local` | 200 | The config server serves the bundle |
| wiremock health | 200 | WireMock is up |
| keymanager health | 200 | Keymanager is up and connected to `mosip_keymgr` |
| keymanager `KERNEL:SIGN` certificate | 200 | The keys exist |
| login `globaladmin` on WireMock | token returned | The IAM stubs and the tokens are loaded |
| app health | 200 | admin-service started with the local config |
| masterdata languages, with token | 200 | Token validation and the DB work |
| masterdata languages, no token | 401 | Security is on |
| admin roles, with token | 200 | The admin APIs work |
| sample test data: zone hierarchy contains `MOR` | found | The sample test data is loaded |
| admin keymanager proxy `KERNEL:SIGN`, with token | 200 | The app reaches keymanager |

Checks you can run by hand:

- `./run-local-stack.sh status`: all four containers `healthy`. `masterdata-loader` is not listed because it runs once and exits.
- App logs: `Started AdminBootApplication`, and no `Could not resolve placeholder` errors.
- A call with a token: see the "Authentication" section.

## Database

PostgreSQL 18 runs in the `postgres` container. Its data is kept in the Docker volume `admin-local_postgres-data`, so it survives `down` and restarts; only `wipe` removes it.

| Database | Schema | User / password | Built from |
|---|---|---|---|
| `mosip_master` | `master` | `masteruser` / `mosip123` | `../../db_scripts/mosip_master`, plus the sample test data from `deps/mosip-data` |
| `mosip_keymgr` | `keymgr` | `keymgruser` / `mosip123` | `deps/keymanager/db_scripts/mosip_keymgr` |

The admin user for both is `postgres` / `mosip123`. The databases are built once, on the first `up` or after `wipe`; a change to the DB scripts is picked up on the next `wipe`.

**Connect**

| Setting | Value |
|---|---|
| Host / port | `localhost` / `5456` |
| Database | `mosip_master` or `mosip_keymgr` |
| User / password | `postgres` / `mosip123` |
| JDBC URL | `jdbc:postgresql://localhost:5456/mosip_master` |

From the command line (no local `psql` needed):

```bash
docker exec -it admin_local_postgres psql -U postgres -d mosip_master
```

Or with a local `psql`: `psql -h localhost -p 5456 -U postgres -d mosip_master`. Any SQL client (DBeaver, IntelliJ Database, pgAdmin) works with the settings above.

## Authentication

There is no Keycloak. WireMock acts as the IAM and hands out ready-made tokens (valid for 100 years).

**Where the tokens are**

| File | What it is |
|---|---|
| `tools/jwt/tokens.properties` | The list of users, clients and their roles. Edit this one |
| `deps/wiremock/mappings/keycloak-authmanager.json` | The generated tokens that WireMock serves. Committed; never edit by hand |
| `keys/jwt-signing-key.pem` | The key that signs the tokens. Created locally; not committed |

**When to recreate the tokens**

Only after you change `tools/jwt/tokens.properties` (a new user, client or role):

```bash
./run-local-stack.sh tokens
```

Then restart admin-service and commit `tokens.properties` and `keycloak-authmanager.json`. Not needed after a clone, `up`, `down` or `wipe`.

**How to get a token**

```bash
curl -si -X POST http://localhost:8083/v1/authmanager/authenticate/internal/useridPwd \
  -H 'Content-Type: application/json' -d '{"request":{"userName":"globaladmin"}}' | grep -i '^authorization:'
```

Send it as the cookie `Authorization=<token>`. Other users are listed in `tokens.properties`. More details: `tools/jwt/README.md`.

## API test rig

The API test rigs run their test cases against the local stack. Each rig calls WireMock, which forwards the calls to admin-service:

```text
rig ──► WireMock :8083 ──► gateway stubs (deps/wiremock/mappings/api-test-rig.json) ──► admin-service :8086
```

No rig files need changing: the settings below override the rig's own properties.

### 1. Masterdata rig

Tests the masterdata and admin APIs (`/v1/masterdata/**`, `/v1/admin/**`) in three languages (eng, ara, fra).

**Before you run it:** `./run-local-stack.sh up`, start admin-service on 8086, then `./run-local-stack.sh smoke` (every line `[ OK ]`).

**Run from the IDE.** Create a Java application run configuration:

| Field | Value |
|---|---|
| Main class | `io.mosip.testrig.apirig.masterdata.testrunner.MosipTestRunner` |
| Working directory | `api-test` |
| VM options | `-Dmodules=masterdata -Denv.user=api-internal.local -Denv.endpoint=http://localhost:8083 -Denv.testLevel=smokeAndRegression` |
| Environment variables | `keycloak-external-url=http://localhost:8083`, `db-server=localhost`, `db-port=5456`, `postgres-password=mosip123`, `mosip_components_base_urls=` (empty) |

**Run from the command line** (bash: macOS, Linux, or Git Bash on Windows), in the `api-test` folder:

1. Build once and save the classpath:
   ```bash
   mvn -q package -DskipTests
   mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
   ```
2. Run it (the first line picks the classpath separator: `:` on macOS and Linux, `;` on Windows):
   ```bash
   SEP=':'; [[ "$OS" == "Windows_NT" ]] && SEP=';'
   env 'keycloak-external-url=http://localhost:8083' 'db-server=localhost' 'db-port=5456' \
       'postgres-password=mosip123' 'mosip_components_base_urls=' \
     java -Dmodules=masterdata -Denv.user=api-internal.local -Denv.endpoint=http://localhost:8083 \
       -Denv.testLevel=smokeAndRegression -cp "target/classes${SEP}$(cat target/cp.txt)" \
       io.mosip.testrig.apirig.masterdata.testrunner.MosipTestRunner
   ```
   A full run takes a few minutes.

**Results:** one report per language in `api-test/testng-report/`, named `mosip-api-internal.local-masterdata-<lang>-…-full-report_T-…_P-…_S-…_F-…_I-…_KI-….html`; the counts are in the file name.

**Expected result:** eng F-0; ara and fra F-0 with I-15; S-0 everywhere.

#### Expected failures on the first run

On a fresh database (after the first `up`, or after `wipe`), the eng report shows **F-2**. These two failures are expected, so treat them as excluded for that run. They come from the rig's test data, not from admin-service, and they happen on any new environment:

| Test | Error | Why |
|---|---|---|
| `Admin_GetMachineHistory_allValid_smoke` | `KER-MSD-032 Machine History not found` | It uses the fixed machine id `10001`. On a new DB the eng run creates machine `10000`; `10001` is created only later, by the ara run |
| `MasterData_GetUserDetailsByIdAndEffDate_allValid_smoke` | `KER-USR-003 User History not found` | It queries user history at the mapping call's `responsetime`, a few ms before the history row is saved |

From the second run on the same database, both pass because history from the previous run exists. Any other failure on the first run, or either of these two on a later run, is a real failure.

## Keymanager

admin-service uses keymanager to sign data and to serve its certificates (`/v1/admin/keymanager/**`). Locally it is the real keymanager, release `1.4.1`, with a keystore file instead of a hardware security module.

| Item | Where |
|---|---|
| Container | `keymanager`, host port 8089 (image `mosipid/kernel-keymanager-service:1.4.1`) |
| Keys | Keystore `admin-local-ks.p12` in the Docker volume `admin-local_keymanager-keys` |
| Key records | Database `mosip_keymgr`, built from `deps/keymanager/db_scripts` (keymanager tag `v1.4.1`) |
| Start-up | `deps/keymanager/bootstrap.sh`: creates the `ROOT`, `KERNEL` and `KERNEL:SIGN` keys on the first start and reuses them afterwards |
| Config | `deps/mosip-config/keymanager-local.properties` |

**Good to know**

- The keystore and `mosip_keymgr` belong together. `up`, `down` and restarts keep both; `wipe` resets both. Never remove only one of them.
- Its own APIs on 8089 do not check tokens. Calls through admin-service still do.
- Start-up messages start with `keymanager-bootstrap:` (`./run-local-stack.sh logs`); errors are explained under "Troubleshooting".

More details, and how to move to a new keymanager release: `deps/keymanager/README.md`.

## Rules

- Local changes go in the `*-local.properties` overlays. Keep the `*-default` files, `identity-mapping.json`, `applicanttype.mvel` and `deps/mosip-data/xlsx` identical to their source repos, and refresh them by copying.
- Change a served config value in the overlay, not as a `-D` option, because the config server overrides `-D` options. Only `server.port` and `spring.cloud.config.uri` work as `-D` options.
- Tokens: edit `tools/jwt/tokens.properties` and run `tokens`. Never edit `keycloak-authmanager.json` by hand. The JWT issuer must stay `http://localhost:8083/auth/realms/mosip`.
- Commit `keycloak-authmanager.json` only together with a `tokens.properties` change. Never commit `keys/jwt-signing-key.pem`.
- After editing a WireMock mapping file, recreate the `wiremock` container or run `curl -X POST http://localhost:8083/__admin/mappings/reset`.
- Reset the database and the keystore only with `wipe`, which removes both volumes together. Never `docker volume rm` one of them.
- Pin every image to a version (`postgres:18.6-bookworm`, `kernel-config-server:1.3.1`, `wiremock:3.13.2`, `kernel-keymanager-service:1.4.1`, `masterdata-loader:1.3.1`). Never use `latest` or `develop`.
- Keep `run-local-stack.sh` and `run-local-stack.bat` in step: same commands, ports and checks.
- Use port 8096 for a second app instance next to the IDE; the IDE keeps 8086.
- Never: run the app in Compose, use `{cipher}` values, copy secrets from a real environment, or add the auth adapter to the keymanager container.

## Troubleshooting

When `up` reports a container as `unhealthy` or `exited`, check `./run-local-stack.sh status`, then read that container's log with `./run-local-stack.sh logs <svc>` (`postgres`, `config-server`, `wiremock`, `keymanager`). Commands below run from `local-dev-setup/`; on Windows use `run-local-stack.bat`.

After any fix, run `./run-local-stack.sh smoke` (add the port if the app is not on 8086); every line should be `[ OK ]`.

### Stack errors

| Symptom | Cause | Steps to fix |
|---|---|---|
| `Docker is not running` / `'docker' not found on PATH` | Docker Desktop is stopped, or its CLI is not on `PATH` | 1. Start Docker Desktop and wait until `docker info` answers.<br>2. If `docker` is not found on macOS, add `$HOME/.docker/bin` to `PATH`.<br>3. `./run-local-stack.sh up` |
| `Bind for 0.0.0.0:<port> failed: port is already allocated` | Another process or stack uses 5456, 51200, 8083 or 8089 | 1. `lsof -nP -iTCP:<port> -sTCP:LISTEN` to see what holds the port.<br>2. If it is a container of another stack: `docker ps --filter publish=<port>`, then `docker stop <name>`. Otherwise stop that process.<br>3. `./run-local-stack.sh up`<br>4. `status`: all four containers `healthy` |
| postgres `exited` on the first `up` or `wipe` | `init.sql` or a `db_scripts` file failed; the init stops at the first error | 1. `./run-local-stack.sh logs postgres` and find `psql:<file>:<line>: ERROR`.<br>2. Fix that file.<br>3. `./run-local-stack.sh wipe`: a failed init leaves a half-built database that a plain `up` does not rebuild.<br>4. `status`: postgres `healthy` |
| config-server `unhealthy` | Usually a slow start (Docker short of CPU or memory). Rarely, an exception at start | 1. `./run-local-stack.sh logs config-server`.<br>2. Still starting (no exception): run `./run-local-stack.sh up` again.<br>3. An exception: check that `deps/mosip-config` exists and is readable, then `up`.<br>4. `curl -s -o /dev/null -w '%{http_code}' http://localhost:51200/config/admin/default,local` returns `200` |
| The app does not see a config change | The file name or profile is wrong, or the app was not restarted | 1. `curl -s http://localhost:51200/config/admin/default,local \| grep <key>` to see whether the value is served.<br>2. Not served: the file must be `deps/mosip-config/admin-local.properties`, and the app must run with `-Dspring.profiles.active=default,local`.<br>3. Restart admin-service (the config server needs no restart) |
| wiremock `exited`, log shows `MappingFileException: Error loading file .../<name>.json` | A mapping file is not valid JSON | 1. `python3 -m json.tool deps/wiremock/mappings/<name>.json` to find the line.<br>2. Fix the file (for `keycloak-authmanager.json`, fix `tools/jwt` and run `tokens` instead).<br>3. `./run-local-stack.sh up`<br>4. `status`: wiremock `healthy` |
| WireMock returns 404 for a call | No stub matches the call | 1. `curl -s http://localhost:8083/__admin/requests/unmatched` to see the method, URL and body.<br>2. Add a stub to `deps/wiremock/mappings/admin-services.json`.<br>3. `curl -X POST http://localhost:8083/__admin/mappings/reset` to reload.<br>4. Repeat the call |
| API returns 401 with a token | The token was signed with another key than the JWKS admin-service loaded (a new key was created, or a changed `keycloak-authmanager.json` was pulled), or the user is not in `tokens.properties` | 1. Reload WireMock: `curl -X POST http://localhost:8083/__admin/mappings/reset`.<br>2. Restart admin-service so it reloads the JWKS.<br>3. Get a new token (see the "Authentication" section) and repeat the call.<br>4. Still 401: add the user and roles to `tools/jwt/tokens.properties`, run `./run-local-stack.sh tokens`, then steps 2 and 3 |
| `tokens`: `AccessDeniedException: keys` or `keys/jwt-signing-key.pem` | `keys/` or the key file is not writable by you (for example created by another user or with `sudo`) | 1. `ls -ld keys keys/jwt-signing-key.pem`<br>2. `sudo chown "$(id -u):$(id -g)" keys` (and the key file if it exists); on Windows `attrib -r keys\jwt-signing-key.pem`.<br>3. `./run-local-stack.sh tokens` again. Details: `tools/jwt/README.md` |
| `up` stops after `Loading sample test data`, or the `smoke` sample test data check fails | The loader failed (its error is printed by `up`) or did not run | 1. Read the loader error in the `up` output.<br>2. Run it again: `docker compose -f deps/docker-compose.yml --profile seed run --rm masterdata-loader`<br>3. Still failing, or the database is in doubt: `./run-local-stack.sh wipe`<br>4. `smoke`: sample test data check `[ OK ]` |

### Keymanager errors

Every line from `deps/keymanager/bootstrap.sh` starts with `keymanager-bootstrap:`; read them with `./run-local-stack.sh logs`.

| Symptom | Cause | Steps to fix |
|---|---|---|
| keymanager `unhealthy` | One of the errors below | 1. `./run-local-stack.sh logs`<br>2. Find the `keymanager-bootstrap: ERROR` line and follow its row |
| `keymanager-bootstrap: ERROR: keymanager exited` | Keymanager stopped while starting: usually a value is missing from `keymanager-local.properties` (`Could not resolve placeholder`), or `mosip_keymgr` cannot be reached | 1. In the log, find the first `Caused by` above the error.<br>2. `Could not resolve placeholder 'x'`: add `x` to `deps/mosip-config/keymanager-local.properties`. A database error: check that postgres is `healthy`.<br>3. `docker compose -f deps/docker-compose.yml restart keymanager`<br>4. `status`: keymanager `healthy` |
| `keymanager-bootstrap: ERROR: {...}` after `generateMasterKey ...` | Keymanager refused to create a key. With a `KER-KMA-...` code, the keystore and `mosip_keymgr` are out of step | 1. `./run-local-stack.sh wipe` (resets both together).<br>2. Same error again: check that the keystore settings in `keymanager-local.properties` were not changed.<br>3. `status`: keymanager `healthy` |
| `keymanager-bootstrap: ERROR: KERNEL:SIGN certificate not available` | The keys were created but cannot be read back | 1. `./run-local-stack.sh wipe`<br>2. `status`: keymanager `healthy` |
| `keymanager-bootstrap: ERROR: health timeout` | No healthy response within 4 minutes: Docker short of CPU or memory, or config-server not answering | 1. `status`: config-server must be `healthy`; if not, follow its row.<br>2. `docker stats --no-stream`: give Docker Desktop at least 4 GB (the stack uses about 1.8 GB) and stop unrelated containers.<br>3. `docker compose -f deps/docker-compose.yml restart keymanager`<br>4. `status`: keymanager `healthy` |
| `KER-KMA-...` errors from the app's `/v1/admin/keymanager/**` calls | The keystore and `mosip_keymgr` are out of step (one of the two volumes was removed by hand) | 1. `./run-local-stack.sh wipe`<br>2. `smoke`: keymanager checks `[ OK ]` |
| `up`: `service "keymanager-keys-init" didn't complete successfully` | The one-off `chown` on the keystore volume failed, so keymanager is not started | 1. `docker logs admin_local_keymanager_keys_init`<br>2. Check that Docker can run `mosipid/kernel-keymanager-service:1.4.1` (image pulled, enough disk).<br>3. `./run-local-stack.sh up` |
| keymanager log: `AccessDeniedException` or `Permission denied` on `/home/mosip/keys/admin-local-ks.p12` | The keystore volume is not owned by uid 1001 (`keymanager-keys-init` did not run) | 1. `docker compose -f deps/docker-compose.yml run --rm keymanager-keys-init`<br>2. `docker compose -f deps/docker-compose.yml restart keymanager`<br>3. `status`: keymanager `healthy` |

### App and script errors

| Symptom | Cause | Steps to fix |
|---|---|---|
| App: `Could not resolve placeholder 'x'` | A value the cluster injects is missing | 1. Add `x` to `deps/mosip-config/admin-local.properties`.<br>2. Restart admin-service |
| App: `Connection refused: localhost:51000` | The config server address is missing, so the default port is used | 1. Add `-Dspring.cloud.config.uri=http://localhost:51200/config` to the VM options.<br>2. Restart admin-service |
| App: `No servers available for service: localhost` | The `local` overlay is not applied, so `spring.cloud.loadbalancer.enabled=false` is missing | 1. Check the VM option `-Dspring.profiles.active=default,local`.<br>2. `curl -s http://localhost:51200/config/admin/default,local \| grep loadbalancer` shows the value.<br>3. Restart admin-service |
| `Port 8086 already in use` | Another instance is running | 1. `lsof -nP -iTCP:8086 -sTCP:LISTEN` to find it.<br>2. Stop it, or run the second instance with `run 8096` and check it with `smoke 8096` |
| `run`: `admin-service jar not found` | The jar is not built | 1. `./run-local-stack.sh build`<br>2. `./run-local-stack.sh run` |
| `run`: `config server not reachable` | The stack is not up | 1. `./run-local-stack.sh up`<br>2. `./run-local-stack.sh run` |
| `smoke`: a `[FAIL]` line | The check names the part that failed (see the "Verify the stack" section) | 1. A dependency check (config-server, wiremock, keymanager): follow that container's row above.<br>2. `login globaladmin on WireMock`: WireMock is down or the tokens are missing; run `tokens`.<br>3. App `health` returns `000`: admin-service is not running on that port; start it or pass the port to `smoke`.<br>4. `languages (token)` returns 401: see "API returns 401 with a token".<br>5. Sample test data: see the sample test data row.<br>6. Keymanager proxy: see "Keymanager errors" above |

## Layout

```text
local-dev-setup/
├─ run-local-stack.sh / .bat       stack and app commands (bash / Windows)
├─ deps/
│  ├─ docker-compose.yml           postgres, config-server, wiremock, keymanager, masterdata-loader
│  ├─ init.sql                     mosip_master + mosip_keymgr
│  ├─ mosip-config/                config served by the config server
│  ├─ mosip-data/xlsx/             sample test data, loaded by masterdata-loader
│  ├─ wiremock/mappings/           keycloak-authmanager.json (generated), admin-services.json, api-test-rig.json
│  └─ keymanager/                  bootstrap.sh, db_scripts/mosip_keymgr (keymanager v1.4.1)
├─ keys/
│  └─ jwt-signing-key.pem          signs the WireMock IAM tokens (created by tokens, gitignored)
└─ tools/jwt/                      token generator and token definitions
```
