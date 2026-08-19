package io.mosip.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Re-applies each merged service's URL prefix per controller package.
 *
 * <p>
 * Before the merge each service owned a {@code server.servlet.context-path}
 * ({@code /v1/admin}, {@code /v1/hotlist}, {@code /v1/masterdata}, {@code /v1/syncdata}) and
 * its controllers mapped relative to it. One Spring Boot application has exactly one
 * context-path, so the merged app runs with none and re-introduces the prefixes here. Every
 * external path is therefore preserved without editing a single {@code @RequestMapping}.
 * </p>
 *
 * <p>
 * Istio passes the full prefixed path through without a rewrite, so the application has to
 * own these prefixes.
 * </p>
 *
 * <p>
 * {@link #ADMIN_BASE_PACKAGES} intentionally includes {@code io.mosip.kernel.authcodeflowproxy}.
 * Its {@code LoginController} is component-scanned by admin-service and declares no
 * class-level {@code @RequestMapping}, so {@code /login/**}, {@code /login-redirect/**},
 * {@code /logout/user} and {@code /authorize/admin/validateToken} were served under
 * {@code /v1/admin} by the old context-path. Omitting the package here would silently move
 * the admin UI's login endpoints to the server root.
 * </p>
 */
@Configuration
public class ApiPathPrefixConfig implements WebMvcConfigurer {

	/** External URL prefix that admin-service's controllers used to get from the context-path. */
	public static final String ADMIN_PREFIX = "/v1/admin";

	private static final String[] ADMIN_BASE_PACKAGES = { "io.mosip.admin", "io.mosip.kernel.authcodeflowproxy" };

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(ADMIN_PREFIX, HandlerTypePredicate.forBasePackage(ADMIN_BASE_PACKAGES));
		// Steps 1-3 add: /v1/masterdata, /v1/syncdata, /v1/hotlist.
	}
}
