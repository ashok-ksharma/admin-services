# Config server — running with `subdomain`

## Command

Run from `C:\MOSIP_Project_Code\Utilities\config-server`:

```
java -jar -Dspring.profiles.active=native -Dspring.cloud.config.server.native.search-locations="file:C:\MOSIP_Project_Code\admin-services-merge\config-test\mosip-config" -Dspring.cloud.config.server.accept-empty=true -Dspring.cloud.config.server.git.force-pull=false -Dspring.cloud.config.server.git.cloneOnStart=false -Dspring.cloud.config.server.git.refreshRate=0 -Dspring.cloud.config.server.overrides.subdomain=dev kernel-config-server-1.2.1.0-20240620.114036-1.jar
```

`-Dsubdomain=dev` does **not** work. The value must be passed as
`-Dspring.cloud.config.server.overrides.subdomain=dev`.

## Verifying the substitution

Use the plain-text endpoint — it resolves placeholders:

```
http://localhost:51000/config/{name}/{profile}/{label}/{filename}
```

Example:

```
http://localhost:51000/config/admin/dev/dev/application-dev.properties
```

Expected:

```
keycloak.external.url=https://iam.dev.mosip.net
mosip.api.public.host=api.dev.mosip.net
mosip.api.internal.host=api-internal.dev.mosip.net
```

## Do not use the JSON endpoint to verify

```
http://localhost:51000/config/admin/dev/dev
```

This always returns raw values:

```
"mosip.api.internal.host":"api-internal.${subdomain}.mosip.net"
```

That is correct output, not a failure. The JSON endpoint ships values verbatim plus an
`overrides` property source containing `subdomain=dev`; the client resolves them.
