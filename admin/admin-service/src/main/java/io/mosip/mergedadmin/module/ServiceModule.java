package io.mosip.mergedadmin.module;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import jakarta.servlet.http.HttpServletRequest;

/**
 * How one merged service plugs into the application's web layer. Each service declares one
 * {@code ServiceModule} bean in its own package; {@code ApiPathPrefixConfig} and
 * {@code UnmappedPathExceptionHandler} read the declarations, so merging a service means adding
 * its declaration rather than editing those classes.
 *
 * @param name                   short service name, for logs and test messages
 * @param urlPrefix              external URL prefix of the service's controllers, e.g. {@code /v1/admin}
 * @param controllerPackages     packages whose controllers get {@code urlPrefix}
 * @param unmappedPathPrefixes   URL prefixes whose unmapped requests this service answers; the
 *                               longest matching prefix across all services wins
 * @param unmappedRequestHandler the service's answer to an unmapped request under those prefixes
 */
public record ServiceModule(String name, String urlPrefix, List<String> controllerPackages,
		List<String> unmappedPathPrefixes, UnmappedRequestHandler unmappedRequestHandler) {

	/** A service's response to a request that no controller maps. */
	@FunctionalInterface
	public interface UnmappedRequestHandler {

		/**
		 * @return the service's error response, or {@code null} when the service's exception
		 *         handler is not in this application context (per-service test contexts), in
		 *         which case Spring's default 404 is returned
		 */
		ResponseEntity<ResponseWrapper<ServiceError>> handle(HttpServletRequest request,
				NoResourceFoundException exception) throws IOException;
	}
}
