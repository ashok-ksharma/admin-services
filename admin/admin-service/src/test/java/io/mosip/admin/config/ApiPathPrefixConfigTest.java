package io.mosip.admin.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import io.mosip.admin.TestBootApplication;

/**
 * Guards the external URL contract after the context-path collapse.
 *
 * <p>
 * admin-service used to get {@code /v1/admin} from {@code server.servlet.context-path}, which
 * applied to every controller in the application automatically. The merged application has no
 * context-path and instead prefixes per controller package via {@link ApiPathPrefixConfig}, so
 * a controller in a package that is scanned but not listed there would silently start serving
 * from the server root. These tests fail if that happens.
 * </p>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestBootApplication.class)
public class ApiPathPrefixConfigTest {

	/**
	 * Boot's own {@code /error} mapping is the one handler legitimately left at the server
	 * root. springdoc is deliberately NOT exempt: its UI and api-docs are pinned back under
	 * {@code /v1/admin} so the documentation URLs survive the context-path removal.
	 */
	private static final List<String> NON_APPLICATION_HANDLER_PACKAGES = List.of("org.springframework");

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	@Autowired
	private WebEndpointProperties webEndpointProperties;

	@Autowired
	private WebMvcEndpointHandlerMapping webMvcEndpointHandlerMapping;

	@Test
	public void everyApplicationEndpointIsServedUnderTheAdminPrefix() {
		Set<String> unprefixed = applicationPatterns().stream()
				.filter(pattern -> !pattern.startsWith(ApiPathPrefixConfig.ADMIN_PREFIX))
				.collect(Collectors.toSet());

		assertTrue("Endpoints are not served under " + ApiPathPrefixConfig.ADMIN_PREFIX
				+ ", so their external URL changed: " + unprefixed, unprefixed.isEmpty());
	}

	@Test
	public void knownAdminEndpointsKeepTheirPreMergeExternalPaths() {
		Set<String> patterns = applicationPatterns();

		// Sampled across the controllers that had no class-level @RequestMapping and therefore
		// relied entirely on the context-path for their /v1/admin prefix.
		for (String expected : List.of("/v1/admin/roles", "/v1/admin/bulkupload", "/v1/admin/lostRid",
				"/v1/admin/packetstatusupdate", "/v1/admin/masterdata/**")) {
			assertTrue("Expected mapping " + expected + " is missing; mapped patterns were " + patterns,
					patterns.contains(expected));
		}
		assertFalse("Un-prefixed /roles is still mapped - the prefix is not being applied",
				patterns.contains("/roles"));
	}

	/**
	 * The API documentation used to sit under the context-path. springdoc has no notion of
	 * the per-package prefix, so its paths are pinned explicitly; this checks they landed.
	 */
	@Test
	public void swaggerUiAndApiDocsStayUnderTheAdminPrefix() {
		Set<String> patterns = applicationPatterns();

		for (String expected : List.of("/v1/admin/swagger-ui.html", "/v1/admin/v3/api-docs")) {
			assertTrue("Expected springdoc mapping " + expected + " is missing; mapped patterns were "
					+ patterns, patterns.contains(expected));
		}
	}

	/**
	 * Actuator is served by its own handler mapping, which the per-package prefix does not
	 * touch, so the prefix comes from management.endpoints.web.base-path. The Helm
	 * liveness/readiness probes and the Prometheus scrape path depend on this.
	 */
	@Test
	public void actuatorStaysUnderTheAdminPrefix() {
		assertEquals("Actuator base-path changed; the Helm probe paths would 404",
				ApiPathPrefixConfig.ADMIN_PREFIX + "/actuator", webEndpointProperties.getBasePath());

		Set<String> actuatorPatterns = new java.util.HashSet<>();
		webMvcEndpointHandlerMapping.getHandlerMethods().keySet()
				.forEach(info -> actuatorPatterns.addAll(info.getPatternValues()));

		assertFalse("No actuator endpoints were mapped, so this test proves nothing",
				actuatorPatterns.isEmpty());
		Set<String> unprefixed = actuatorPatterns.stream()
				.filter(pattern -> !pattern.startsWith(ApiPathPrefixConfig.ADMIN_PREFIX + "/actuator"))
				.collect(Collectors.toSet());
		assertTrue("Actuator endpoints served outside " + ApiPathPrefixConfig.ADMIN_PREFIX + "/actuator: "
				+ unprefixed, unprefixed.isEmpty());
	}

	/** Mapped URL patterns of controllers this application owns. */
	private Set<String> applicationPatterns() {
		Set<String> patterns = new java.util.HashSet<>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : requestMappingHandlerMapping.getHandlerMethods()
				.entrySet()) {
			if (isFrameworkHandler(entry.getValue())) {
				continue;
			}
			RequestMappingInfo info = entry.getKey();
			if (info.getPathPatternsCondition() != null) {
				info.getPathPatternsCondition().getPatternValues().forEach(patterns::add);
			}
			if (info.getPatternsCondition() != null) {
				patterns.addAll(info.getPatternsCondition().getPatterns());
			}
		}
		return patterns;
	}

	private boolean isFrameworkHandler(HandlerMethod handlerMethod) {
		String declaringPackage = handlerMethod.getBeanType().getName();
		return NON_APPLICATION_HANDLER_PACKAGES.stream().anyMatch(declaringPackage::startsWith);
	}
}
