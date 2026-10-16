# AGENTS.md

This file provides guidance to AI agents when working with code in `admin/admin-service`.

## What This Module Is

`admin-service` is one Spring Boot application made of several service modules. Each module has its own
package, URL prefix, error format and Swagger group, and they share one web layer and one `mosip_master`
datasource.

| Module | URL prefix | Package | Role |
|---|---|---|---|
| admin | `/v1/admin` | `io.mosip.admin` | Bulk upload, packet status tracking, RID recovery, applicant details, admin UI login |
| masterdata | `/v1/masterdata` (also served under `/v1/admin/masterdata`) | `io.mosip.masterdata` | CRUD for all reference/master data |

## Build & Run

```bash
cd admin/admin-service
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true
java -jar target/admin-service-*.jar
```

- Main class: `io.mosip.mergedadmin.AdminBootApplication`.
- Configuration comes from the config server (`bootstrap.properties`: `spring.cloud.config.name=admin`).
- Swagger UI: `/v1/admin/swagger-ui.html` (one UI, one group per module); masterdata's group directly:
  `/v1/masterdata/swagger-ui.html`.
- Actuator: `/v1/admin/actuator`.

## Tests

```bash
mvn test                                        # all unit tests (H2, no external services)
mvn test -Dtest=FullApplicationContextTest      # the whole application assembled
mvn test -Dtest=ApplicantTypeControllerTest     # a single test class
```

- Tests mirror the main packages; each test sits in the package of the class it tests.
- Each module has its own test application: `io.mosip.admin.TestBootApplication`,
  `io.mosip.masterdata.TestBootApplication`. `io.mosip.mergedadmin.FullApplicationContextTest` starts the
  complete application and checks how the modules fit together (URL prefixes, error shapes, Swagger groups,
  shared beans).

## Packages — what goes where

| Package | Holds |
|---|---|
| `io.mosip.admin` | admin module code: controllers, services, entities, DTOs, error codes, exception handler, response advice, filters and audit specific to admin. Its module declarations: `config.AdminServiceModuleConfig`, `config.AdminSwaggerConfig` |
| `io.mosip.masterdata` | masterdata module code, same split. Its module declarations: `config.MasterdataServiceModuleConfig`, `config.MasterDataSwaggerConfig`. Also `config.MasterDataSourceConfig`: the `mosip_master` datasource, entity manager and transaction manager used by admin and masterdata |
| `io.mosip.mergedadmin` | Application-level code shared by all modules, nothing module-specific: |
| `  (root)` | `AdminBootApplication` (component scan of all modules) |
| `  .module` | `ServiceModule` — how a module declares its URL prefix, controller packages and unmapped-request handling |
| `  .web` | `ApiPathPrefixConfig` (URL prefix per controller package), `UnmappedPathExceptionHandler` (unmapped request → owning module's error handler), `AntPathMatcherConfig` |
| `  .openapi` | `SwaggerConfig` (the application's OpenAPI description and Swagger UI), `OpenApiProperties` |
| `  .httpfilter` | `HttpFilterConfig`, `ReqResFilter` (request filters) |
| `  .config` | `LocalDateLocalTimeConfig` (the `@Primary ObjectMapper`), `LoggerConfiguration` |

Within a module, the layering of the repository-level `AGENTS.md` applies (controller → service →
repository, `dto/`, `entity/`, `exception/`, `RequestWrapper`/`ResponseWrapper`).

## How the application is wired

- **URL prefixes**: there is no `server.servlet.context-path`. `ApiPathPrefixConfig` adds each module's prefix to
  the controllers in its packages, as declared by the module's `ServiceModule` bean. Controller
  `@RequestMapping`s are relative (e.g. `/machines`, not `/v1/masterdata/machines`).
- **Unmapped requests**: `UnmappedPathExceptionHandler` sends a request that no controller maps to the error
  handler of the module owning its URL prefix (longest prefix wins), e.g. `KER-MSD-500` under
  `/v1/masterdata` and `/v1/admin/masterdata`. Anything else gets Spring's 404.
- **Swagger**: `SwaggerConfig` provides the one OpenAPI description and UI; each module adds its own
  `GroupedOpenApi` bean in its own package (springdoc collects them).
- **Request filters** (`HttpFilterConfig`): `logFilter` and `ReqResFilter` run for every request. Both modules'
  response advices read the request body that `ReqResFilter` caches.
- **JSON**: one `@Primary ObjectMapper` (`LocalDateLocalTimeConfig`) for all modules — `LocalDate`
  `yyyy-MM-dd`, `LocalTime` `HH:mm:ss`, `LocalDateTime` `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`, unknown JSON fields
  ignored.
- **URL matching**: `spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER` (config server,
  `application-default`). The `AntPathMatcher` bean in `AntPathMatcherConfig` is used only by
  `kernel-authcodeflowproxy-api`'s `LoginController` to check login redirect URLs.
- **Database**: `MasterDataSourceConfig` is the only datasource/entity manager/transaction manager
  (`mosip_master`). It lists the repository and entity packages of the modules that use this database, and
  the paths that use open-session-in-view (`/v1/admin/**`, `/v1/masterdata/**`). Kernel's `HibernateDaoConfig`
  and Spring Boot's datasource auto-configuration are deliberately not used.

## Do's and Don'ts

**Do**
- Keep module code in its module's package; put something in `io.mosip.mergedadmin` only if every module uses
  it unchanged and it holds no module-specific value (URL, package name, error code, table name).
- Keep a module's error handling, response advices and filters scoped to that module: advices with
  `basePackages`, filters with `FilterRegistrationBean.addUrlPatterns("/v1/<module>/*")`, interceptors with
  `addPathPatterns("/v1/<module>/**")`.
- Keep database configuration with the module that owns the database.
- When renaming or moving a package, update the package names that are written as strings (the compiler does
  not check them), then `grep` for the old name:
  - `@ComponentScan` lists: `AdminBootApplication`, both `TestBootApplication`s, `FullApplicationContextTest`
  - `ServiceModule` controller packages
  - `MasterDataSourceConfig` repository and entity packages
  - advice `basePackages`
  - AOP pointcuts: `DefaultSort`
  - JPQL `SELECT new <class>`: `DynamicFieldRepository`, `RegWorkingNonWorkingRepo`
  - test scan filters in `FullApplicationContextTest`
  - configuration values naming a class: `hibernate.empty.interceptor`
- Move files with `git mv` so their history follows them.
- Keep the line endings of the file you edit: some files use CRLF (e.g. `src/main/resources/bootstrap.properties`).
  Check `git diff --numstat` for unexpected whole-file changes before committing.
- Add tests for module wiring to `FullApplicationContextTest`.

**Don't**
- Don't name a package so that it starts with a module's package name (e.g. `io.mosip.admin.commons`): URL
  prefixes and advices are matched by plain string prefix, so it would get that module's prefix and error
  handling. Don't put application code under `io.mosip.commons` or `io.mosip.kernel` (MOSIP libraries).
- Don't hard-code module prefixes or packages in `ApiPathPrefixConfig`, `UnmappedPathExceptionHandler` or
  `SwaggerConfig`; a module declares them in its own `ServiceModule` and Swagger group beans.
- Don't register a filter as a plain `@Component`/`@Bean` `Filter` (it runs for every URL) or an interceptor
  for `/**` unless it really applies to every module.
- Don't change the shared `ObjectMapper` for one module's needs; give that module its own mapper for its own
  code.
- Don't add another `AntPathMatcher` bean, another `@Primary ObjectMapper`, or another datasource for
  `mosip_master`.
- Don't set `server.servlet.context-path`; prefixes come from `ApiPathPrefixConfig`.

## Adding a service module

*Applies while further services are being brought into this application (syncdata, hotlist, partner
management, registration-processor status and sync services). Remove this section when that is complete.*

1. Keep the service's own package root (`io.mosip.syncdata`, `io.mosip.hotlist`, `io.mosip.pms`,
   `io.mosip.registration.processor.*`). For syncdata, rename `io.mosip.kernel.syncdata` to
   `io.mosip.syncdata` with `git mv`, updating every string-written package name listed above.
2. In the service's own package, add:
   - a `ServiceModule` bean: URL prefix, controller packages, the URL prefixes whose unmapped requests it
     answers, and its handler for them (syncdata `KER-SNC-500`; hotlist `ADM-HTL-015` with HTTP 200). Model
     it on `MasterdataServiceModuleConfig`.
   - a Swagger `GroupedOpenApi` bean, plus a `/v1/<service>/swagger-ui.html` redirect so the old URL keeps
     working. Model it on `MasterDataSwaggerConfig`.
3. In `io.mosip.mergedadmin`: add the service's packages to `AdminBootApplication`'s `@ComponentScan`, and its
   checks to `FullApplicationContextTest`.
4. Database:
   - a service on `mosip_master` (syncdata) adds only its repository package to `MasterDataSourceConfig`;
   - a service with its own database (hotlist, PMS, regproc) brings its own datasource, entity manager and
     transaction manager in its own package, and narrows any broad `io.mosip.*` scan to its own packages.
5. Decide whether `HttpFilterConfig`'s filters should run for the new service; if not, register them for
   `/v1/admin/*` and `/v1/masterdata/*` only. Compare its JSON date formats and unknown-field handling with the
   shared `ObjectMapper`.
6. Scope its own filters, interceptors and advices as described under Do's and Don'ts; check its configuration
   values that hold class or package names.
7. Unit tests, then the API test rig. Update this file.
