package io.mosip.admin.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.mosip.admin.packetstatusupdater.exception.ApiExceptionHandler;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.masterdata.exception.MasterDataApiExceptionHandler;

/**
 * Restores each service's pre-merge response for requests that no controller maps.
 *
 * <p>
 * Such a request falls through to Spring's static-resource handler, which throws
 * {@link NoResourceFoundException}. Before the merge every service's exception advice was
 * unscoped, so its catch-all also covered these requests and answered with that service's
 * {@code *-500} error. In the merged application the advices are scoped to their own
 * controller packages, and an unmapped request has no controller, so none of them applies
 * and Spring's default 404 goes out instead.
 * </p>
 *
 * <p>
 * This advice is unscoped but handles only {@link NoResourceFoundException}, and only for
 * the prefixes of services that have been merged: it hands the request to that service's own
 * catch-all, so the error code and response shape stay owned by the service. That includes
 * unmapped paths under {@code /v1/admin/actuator} and the API documentation, which sat under
 * admin's context-path and were answered by its catch-all too. Any other path - unknown roots
 * and the prefixes of services not yet merged - is rethrown, which leaves Spring's default
 * 404 in place. Add a service's prefix here in the
 * step that merges it ({@code /v1/syncdata}: {@code KER-SNC-500}; {@code /v1/hotlist}:
 * {@code ADM-HTL-015} with HTTP 200).
 * </p>
 *
 * <p>
 * The service advices are looked up lazily because the per-service test contexts only load
 * their own service's advice; a request whose service advice is absent gets the default 404.
 * </p>
 */
@RestControllerAdvice
public class UnmappedPathExceptionHandler {

	private final ObjectProvider<ApiExceptionHandler> adminExceptionHandler;

	private final ObjectProvider<MasterDataApiExceptionHandler> masterDataExceptionHandler;

	public UnmappedPathExceptionHandler(ObjectProvider<ApiExceptionHandler> adminExceptionHandler,
			ObjectProvider<MasterDataApiExceptionHandler> masterDataExceptionHandler) {
		this.adminExceptionHandler = adminExceptionHandler;
		this.masterDataExceptionHandler = masterDataExceptionHandler;
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ResponseWrapper<ServiceError>> handleUnmappedRequest(
			final HttpServletRequest httpServletRequest, NoResourceFoundException e)
			throws IOException, NoResourceFoundException {
		// the full URI as the client sent it - the application has no context-path
		String path = httpServletRequest.getRequestURI();

		// /v1/admin/masterdata serves masterdata's endpoints, so it answers as masterdata does
		if (path.startsWith(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/")
				|| path.startsWith(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/")) {
			MasterDataApiExceptionHandler handler = masterDataExceptionHandler.getIfAvailable();
			if (handler != null) {
				return handler.defaultErrorHandler(httpServletRequest, e);
			}
		} else if (path.startsWith(ApiPathPrefixConfig.ADMIN_PREFIX + "/")) {
			ApiExceptionHandler handler = adminExceptionHandler.getIfAvailable();
			if (handler != null) {
				return handler.defaultErrorHandler(httpServletRequest, e);
			}
		}
		// rethrowing the same exception leaves it unresolved, so Spring answers with its 404
		throw e;
	}
}
