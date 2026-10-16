package io.mosip.mergedadmin.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.mosip.mergedadmin.module.ServiceModule;

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
 * The prefixes and packages come from each service's {@link ServiceModule} declaration
 * (admin: {@code AdminServiceModuleConfig}, masterdata: {@code MasterdataServiceModuleConfig});
 * a merged service adds its own declaration instead of editing this class.
 * </p>
 */
@Configuration
public class ApiPathPrefixConfig implements WebMvcConfigurer {

	/** External URL prefix that admin-service's controllers used to get from the context-path. */
	public static final String ADMIN_PREFIX = "/v1/admin";

	/** External URL prefix that masterdata's controllers used to get from the context-path. */
	public static final String MASTERDATA_PREFIX = "/v1/masterdata";

	private final ObjectProvider<ServiceModule> serviceModules;

	public ApiPathPrefixConfig(ObjectProvider<ServiceModule> serviceModules) {
		this.serviceModules = serviceModules;
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		serviceModules.orderedStream().forEach(module -> configurer.addPathPrefix(module.urlPrefix(),
				HandlerTypePredicate.forBasePackage(module.controllerPackages().toArray(new String[0]))));
	}
}
