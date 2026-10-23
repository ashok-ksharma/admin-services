# Keymanager for local setup

Files for the keymanager container and its database `mosip_keymgr`. Keymanager runs for real, with a PKCS12 keystore instead of SoftHSM.

## Versions in use

| Item | Version | Source |
|---|---|---|
| Image | `mosipid/kernel-keymanager-service:1.4.1` | Docker Hub (amd64 and arm64) |
| DB scripts (`db_scripts/mosip_keymgr`) | tag `v1.4.1` | `keymanager` repo, `db_scripts/mosip_keymgr` |

The image and the DB scripts must always come from the same release.

## What it contains

| Path | Purpose | Edit? |
|---|---|---|
| `bootstrap.sh` | Container entrypoint. Starts keymanager without the auth adapter, waits for health, creates `ROOT`, `KERNEL` and `KERNEL:SIGN` on the first start, then marks the container ready (`/tmp/.km-ready`) | Yes |
| `db_scripts/mosip_keymgr/` | Verbatim copy of the keymanager DB scripts | No |

Files in `db_scripts/mosip_keymgr/` and how they are used:

| File | Used | Purpose |
|---|---|---|
| `role_dbuser.sql` | yes | Creates the `keymgruser` role |
| `db.sql` | yes | Creates the `mosip_keymgr` database and the `keymgr` schema |
| `ddl.sql`, `ddl/*.sql` | yes | Creates the 10 tables, indexes and foreign keys |
| `grants.sql` | yes | Grants `keymgruser` access to the tables |
| `dml.sql`, `dml/keymgr-key_policy_def.csv` | yes | Loads the key policies (validity and pre-expiry days per application) |
| `deploy.sh`, `deploy.properties`, `drop_db.sql`, `drop_role.sql` | no | Used by MOSIP's own DB deployment; kept so the folder stays identical to the source |

Related files outside this folder:

| File | Role |
|---|---|
| `../init.sql` | Runs the used scripts above once, on an empty postgres volume, in `deploy.sh` order |
| `../docker-compose.yml` (`keymanager`, `keymanager-keys-init`) | Image tag, ports (host 8089), mounts, health check; the one-off `chown` of the keystore volume to uid 1001 |
| `../mosip-config/keymanager-local.properties` | Keymanager's local config: keystore, DB, placeholder salts, Spring Security off |
| Docker volume `admin-local_keymanager-keys` | The keystore `/home/mosip/keys/admin-local-ks.p12`, created by keymanager on the first start. Not in the source tree |

## How it works

1. On a fresh volume, postgres runs `init.sql`, which builds `mosip_keymgr` from `db_scripts/mosip_keymgr`: tables and key policies, no keys.
2. `bootstrap.sh` starts keymanager with config from the config server (`keymanager,kernel`, profiles `default,local`).
3. If `KERNEL:SIGN` has no certificate yet, it calls `generateMasterKey` for `ROOT`, `KERNEL` and `KERNEL:SIGN`. Keymanager writes the private keys to the keystore and the key records to `mosip_keymgr`.
4. Keys follow the key policies (for example KERNEL 1,095 days, ROOT 2,920 days). Keymanager renews a key by itself once it is within its pre-expiry days.

## Rules

- Never edit `db_scripts/mosip_keymgr`. It must stay identical to the keymanager repo at the tag in "Versions in use".
- Keep the image tag and the DB scripts tag the same. Pin the image to a release; never `latest`.
- Local settings go in `../mosip-config/keymanager-local.properties`, not as `-D` options in `bootstrap.sh`.
- Never add the auth adapter to the container. Its APIs on 8089 are open locally; calls through admin-service still check tokens.
- The keystore and `mosip_keymgr` belong together. Their volumes are kept by `up`, `down` and restarts; reset both only with `./run-local-stack.sh wipe`, never remove only one.
- The keystore and keys are for local use only; never copy them from, or to, a real environment.

## Refreshing to a new keymanager release

Change these together, all to the same release (for example `v1.4.2` / `1.4.2`):

1. **DB scripts:** replace `db_scripts/mosip_keymgr` with the folder from the `keymanager` repo at the new tag, unchanged.
2. **Image:** set `mosipid/kernel-keymanager-service:<version>` in `../docker-compose.yml`, for both `keymanager` and `keymanager-keys-init`.
3. **`../init.sql`:** only if the new `deploy.sh` runs a script that is not in the `mosip_keymgr` section yet; add it in the same order.
4. **`../mosip-config/keymanager-local.properties`:** only if the new keymanager stops with `Could not resolve placeholder`; add the missing key.
5. **`bootstrap.sh`:** only if the image no longer has `/home/mosip/kernel-keymanager-service.jar`. **`keymanager-keys-init`:** only if the image no longer runs as uid 1001 (`docker run --rm --entrypoint id <image>`).
6. **Versions in the docs:** "Versions in use" above, and the image tag in the main `README.md` ("Keymanager" section and the image pinning rule).

Then test: `./run-local-stack.sh wipe`, start admin-service, run `./run-local-stack.sh smoke`, then run the API test rig.
