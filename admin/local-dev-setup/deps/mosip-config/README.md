# Configuration files for local setup

The config server (native mode) serves the files in this folder, mounted at `/mosip-config`. admin-service and keymanager load their configuration from it with profiles `default,local`.

## Naming convention

| Pattern | Meaning | Edit? |
|---|---|---|
| `<name>-default.properties` | Verbatim copy from the `mosip-config` repo: the configuration every environment starts from | No |
| `<name>-local.properties` | Overlay with the values for running on a developer machine | Yes, all changes go here |
| Other files (`identity-mapping.json`, `applicanttype.mvel`) | Verbatim copies from `mosip-config`, fetched by the app at runtime | No |

`<name>` is the config name a service asks for (`admin`, `kernel`, `application`, `keymanager`). The suffix is the Spring profile: `default` for the shared files, `local` for the overlays.

## Files

| File | Type | Used by | Contents |
|---|---|---|---|
| `application-default.properties` | copy | admin-service, keymanager | Settings shared by all MOSIP services |
| `kernel-default.properties` | copy | admin-service, keymanager | Kernel settings (DB, keymanager, auth, roles) |
| `admin-default.properties` | copy | admin-service | admin-service settings |
| `identity-mapping.json` | copy | admin-service | Identity field mapping (`mosip.admin.identityMappingJson`) |
| `applicanttype.mvel` | copy | admin-service | Applicant type rules (`mosip.kernel.applicantType.mvel.file`) |
| `admin-local.properties` | overlay | admin-service | Localhost URLs and ports, DB, and the values a cluster injects from secrets |
| `keymanager-local.properties` | overlay | keymanager | PKCS12 keystore, DB, placeholder salts, Spring Security off |

There is no `keymanager-default.properties`: keymanager reads `kernel-default` and `application-default`, and its overlay on top.

## Override order

A service loads the files for each config name it asks for, under both profiles. A file under the `local` profile always beats every `default` file; within the same profile, a later config name beats an earlier one.

**admin-service** (`spring.cloud.config.name=application,kernel,admin`), highest first:

1. `admin-local.properties`
2. `admin-default.properties`
3. `kernel-default.properties`
4. `application-default.properties`

**keymanager** (`spring.cloud.config.name=keymanager,kernel`), highest first:

1. `keymanager-local.properties`
2. `kernel-default.properties`
3. `application-default.properties`

To see this order yourself, ask the config server for admin-service's configuration. It lists the files it used under `propertySources`, highest priority first:

```bash
curl -s http://localhost:51200/config/application,kernel,admin/default,local | python3 -m json.tool | grep '"name"'
```

Expected output (the first line is the request itself):

```text
    "name": "application,kernel,admin",
            "name": "file:/mosip-config/admin-local.properties",
            "name": "file:/mosip-config/admin-default.properties",
            "name": "file:/mosip-config/kernel-default.properties",
            "name": "file:/mosip-config/application-default.properties",
```

For keymanager, use `http://localhost:51200/config/keymanager,kernel/default,local`.

### Config server values win over `-D` options

admin-service loads its configuration through Spring Cloud bootstrap, so a key set in any file here wins over the same key passed as a `-D` option or set in the app's own `application*.properties`.

- To change a key that a file here sets, edit the overlay (`admin-local.properties`), not the command line.
- A `-D` option works only for keys that no file here sets. The two you need are `-Dspring.cloud.config.uri` (where the config server is) and `-Dserver.port` (the port the app listens on).

## Making a change

A service reads its configuration only when it starts, so after editing an overlay restart that service. The config server itself needs no restart: it re-reads the files on every request.

| You edited | Restart | How (from `local-dev-setup/`) |
|---|---|---|
| `admin-local.properties` | admin-service, which runs on the host | Stop and start it in the IDE, or stop `./run-local-stack.sh run` (Ctrl+C) and start it again |
| `keymanager-local.properties` | the keymanager container | `docker compose -f deps/docker-compose.yml restart keymanager`, then `./run-local-stack.sh status` until keymanager is `healthy` |
| a `*-default` file or other copy (refresh only) | both | Both rows above |

Steps:

1. Put the key in the overlay of the service that needs it.
2. Check that the config server serves it:
   - admin-service: `curl -s http://localhost:51200/config/admin/default,local | grep <key>`
   - keymanager: `curl -s http://localhost:51200/config/keymanager,kernel/default,local | grep <key>`
3. Restart that service as shown in the table.
4. Run `./run-local-stack.sh smoke`; every line should be `[ OK ]`.

## Rules

- Never edit the `*-default` files or the other copied files. A local value goes in a `-local` overlay, even when it replaces a value from a `-default` file.
- One overlay per service: `<name>-local.properties` for the service's own config name. Do not add `kernel-local` or `application-local`: they would apply to every service that reads `kernel` or `application`, and would beat `admin-default` too.
- Overlays hold only values for a developer machine: localhost addresses, the local DB, and placeholders for secrets. Never real secrets or `{cipher}` values.

## Refreshing the copies

1. Replace the `*-default.properties` files, `identity-mapping.json` and `applicanttype.mvel` with the current versions from the `mosip-config` repo, unchanged.
2. Check whether anything needs to be added to the `-local` overlays: a service that stops with `Could not resolve placeholder` needs that key in its overlay.
3. Restart admin-service and the keymanager container (see "Making a change"), then run `./run-local-stack.sh smoke` and the API test rig.
