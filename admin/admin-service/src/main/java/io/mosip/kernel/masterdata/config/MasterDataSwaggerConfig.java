package io.mosip.kernel.masterdata.config;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.mosip.admin.config.ApiPathPrefixConfig;

/**
 * Keeps masterdata's API documentation reachable, and separate from admin-service's, in
 * the merged application.
 *
 * <p>
 * masterdata used to ship its own {@code SwaggerConfig} and {@code OpenApiProperties}.
 * Both were deleted at step 1b: they bound the same {@code openapi.*} prefix as
 * admin-service's copies, so in one application they would have held identical values
 * while colliding on the bean names {@code openApiProperties}, {@code swaggerConfig},
 * {@code openApi} and {@code groupedOpenApi}. What is genuinely per-service is the
 * <em>group</em>, which is all this class contributes.
 * </p>
 *
 * <p>
 * springdoc serves exactly one Swagger UI per application, at
 * {@code springdoc.swagger-ui.path} - two instances are not supported. Distinct per-service
 * URLs are preserved by pointing the old masterdata URL at the shared UI with this group
 * pre-selected, so {@code /v1/masterdata/swagger-ui.html} still opens masterdata's
 * operations and only masterdata's, exactly as it did before the merge.
 * </p>
 *
 * <p>
 * {@code pathsToMatch} works on the real, prefixed paths because
 * {@link ApiPathPrefixConfig} bakes {@code /v1/masterdata} into the handler mappings that
 * springdoc reads - the controllers themselves are still mapped bare.
 * </p>
 */
@Configuration
public class MasterDataSwaggerConfig implements WebMvcConfigurer {

	/**
	 * Group name shown in the Swagger UI selector. Defaults to the title masterdata's own
	 * {@code openapi.info.title} carried before the merge.
	 */
	@Value("${openapi.group.masterdata.name:Admin Master Service}")
	private String groupName;

	@Value("${openapi.group.masterdata.paths:/v1/masterdata/**}")
	private List<String> groupPaths;

	/** Where springdoc actually serves the single UI; used to build the redirect target. */
	@Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
	private String swaggerUiPath;

	@Bean
	public GroupedOpenApi masterdataGroupedOpenApi() {
		return GroupedOpenApi.builder().group(groupName).pathsToMatch(groupPaths.toArray(new String[0])).build();
	}

	/**
	 * Keeps {@code /v1/masterdata/swagger-ui.html} working. springdoc derives the UI
	 * resource root from {@code springdoc.swagger-ui.path}, so the single UI lives under
	 * {@code /v1/admin}; this redirect sends masterdata's URL there with its own group
	 * selected via the {@code urls.primaryName} parameter that swagger-ui honours.
	 */
	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		String uiRoot = swaggerUiPath.substring(0, swaggerUiPath.lastIndexOf('/'));
		String target = uiRoot + "/swagger-ui/index.html?urls.primaryName="
				+ org.springframework.web.util.UriUtils.encodeQueryParam(groupName, java.nio.charset.StandardCharsets.UTF_8);
		registry.addRedirectViewController(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/swagger-ui.html", target);
	}
}
