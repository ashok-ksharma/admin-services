/**
 * Integration layer of the merged application: the place that plugs each merged service (admin,
 * masterdata and, later, syncdata, hotlist, partner management and the registration-processor
 * status and sync services) into the one application. Merging a service means editing this
 * package:
 * <ul>
 * <li>{@code AdminBootApplication} - component scan of the service's packages;</li>
 * <li>{@code web.ApiPathPrefixConfig} - the service's URL prefix for its controller package;</li>
 * <li>{@code web.UnmappedPathExceptionHandler} - which service's error handler answers unmapped
 * requests under its prefix;</li>
 * <li>{@code openapi.SwaggerConfig} - the application's OpenAPI description and Swagger UI;</li>
 * <li>{@code FullApplicationContextTest} (tests) - checks on the assembled application.</li>
 * </ul>
 *
 * <p>
 * Besides these, only configuration that every service uses unchanged belongs here (for example
 * {@code web.AntPathMatcherConfig}). Everything else stays with the service that owns it: entities,
 * repositories, DTOs, error codes, exception handlers, response advices, filters, audit utilities,
 * and datasource configuration (with the service that owns the database).
 * </p>
 *
 * <p>
 * The package name must not start with any service package name ({@code io.mosip.admin},
 * {@code io.mosip.masterdata}, ...): Spring matches handler packages by plain string prefix, so a
 * package such as {@code io.mosip.admin.commons} would receive admin's URL prefix, response wrapping
 * and error handling.
 * </p>
 */
package io.mosip.mergedadmin;
