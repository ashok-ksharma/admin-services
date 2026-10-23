# Sample test data for local setup

Sample test data loaded into `mosip_master` on a fresh database, as in a sandbox installation.

## Version in use

| Item | Version | Source |
|---|---|---|
| `xlsx/` | tag `v1.3.2` | [mosip-data](https://github.com/mosip/mosip-data) repo, `mosip_master/xlsx` |
| Loader image | `mosipid/masterdata-loader:1.3.1` | Docker Hub; the loader MOSIP environments use |

## What it contains

| Path | Purpose | Edit? |
|---|---|---|
| `xlsx/` | 37 files, one per table: location hierarchy, locations, zones, zone users, registration centers and their working days / holidays, devices, machines, document categories / types, identity schema, UI spec, dynamic fields, templates, languages, titles, reasons, blocklisted words | No |

`../init.sql` first loads the seed data from admin-services' `db_scripts/mosip_master/dml`. That seed data fills other tables than the ones here, so loading this sample test data never overwrites it.

## How it is loaded

- The `masterdata-loader` compose service (profile `seed`) reads `xlsx/`. For every table in its `table_order` that has a file here, it truncates the table (`CASCADE`) and inserts the rows, connecting as `masteruser`.
- `run-local-stack.sh` / `.bat` run it after `up` or `wipe`, only when `master.zone` is empty. So it loads once per fresh database, and data created later is kept across `up`.

## Rules

- Never edit the `xlsx` files here. They must stay identical to `mosip-data` at the version in "Version in use".
- Keep the loader image pinned to a version; never `latest` or `develop`.

## Refreshing to a new mosip-data release

Change these together:

1. **Data:** replace the files in `xlsx/` with `mosip_master/xlsx` from the `mosip-data` repo at the new tag, unchanged.
2. **Loader image:** only if the new release needs a newer loader; set `mosipid/masterdata-loader:<version>` in `../docker-compose.yml`.
3. **Versions in the docs:** "Version in use" above, and the loader tag in the main `README.md` (image pinning rule) if it changed.

Then test: `./run-local-stack.sh wipe`, start admin-service, run `./run-local-stack.sh smoke`, then run the API test rig.
