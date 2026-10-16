/**
 * Shared host for the services merged into this application (admin, masterdata and, later,
 * syncdata, hotlist, partner management and the registration-processor status and sync services).
 *
 * <p>
 * Only host mechanisms belong here: the boot application and configuration that every merged
 * service uses unchanged (path routing, web configuration, and similar). A class is moved here only
 * if it contains no service-specific values (URL, package name, error code, table name) and is
 * used unchanged by two or more services.
 * </p>
 *
 * <p>
 * Everything else stays with the service that owns it: entities, repositories, DTOs, error codes,
 * exception handlers, response advices, filters, audit utilities, and datasource configuration (with
 * the service that owns the database).
 * </p>
 *
 * <p>
 * Dependency rules, enforced by {@code PackageBoundaryTest}:
 * </p>
 * <ul>
 * <li>{@code io.mosip.mergedadmin} must not depend on any service package.</li>
 * <li>{@code io.mosip.masterdata} must not depend on {@code io.mosip.admin}.</li>
 * </ul>
 *
 * <p>
 * The package name must not start with any service package name ({@code io.mosip.admin},
 * {@code io.mosip.masterdata}, ...): Spring matches handler packages by plain string prefix, so a
 * package such as {@code io.mosip.admin.commons} would receive admin's URL prefix, response wrapping
 * and error handling.
 * </p>
 */
package io.mosip.mergedadmin;
