-- admin-services local-dev-setup: databases for the host-run admin-service.
-- Postgres runs this once, on an empty volume (/docker-entrypoint-initdb.d).
-- Both databases are built from their db_scripts, in deploy.sh order:
-- mosip_master from the repo's own db_scripts (mounted at /db_scripts), so a DDL or seed change
-- there is picked up on the next wipe; mosip_keymgr from the keymanager copy in
-- keymanager/db_scripts (mounted at /keymanager).

\set ON_ERROR_STOP on

-- ===================== mosip_master =====================
\set mosipdbname mosip_master
\set dbuname masteruser
\set dbuserpwd '''mosip123'''

-- ddl.sql uses \ir (relative to the file); dml.sql uses \COPY './dml/...' (relative to the cwd).
\cd /db_scripts/mosip_master
\echo '--- mosip_master: role, database, schema'
\i role_dbuser.sql
\i db.sql
\echo '--- mosip_master: ddl'
\i ddl.sql
\echo '--- mosip_master: grants'
\i grants.sql
\echo '--- mosip_master: seed data'
\i dml.sql

-- ===================== mosip_keymgr =====================
-- Tables and key policies only; keymanager/bootstrap.sh creates the keys on the first start.
\set dbuserpwd '''mosip123'''

\cd /keymanager/db_scripts/mosip_keymgr
\echo '--- mosip_keymgr: role, database, schema'
\i role_dbuser.sql
\i db.sql
\echo '--- mosip_keymgr: ddl'
\i ddl.sql
\echo '--- mosip_keymgr: grants'
\i grants.sql
\echo '--- mosip_keymgr: key policies'
\i dml.sql
