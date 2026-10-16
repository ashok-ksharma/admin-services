package io.mosip.admin.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.mergedadmin.openapi.OpenApiProperties;
import io.mosip.mergedadmin.web.ApiPathPrefixConfig;
import io.swagger.v3.oas.models.info.Info;

/**
 * admin-service's Swagger group ({@code openapi.group.*}), alongside masterdata's
 * {@code MasterDataSwaggerConfig}. The application's OpenAPI description and Swagger UI are in
 * {@link io.mosip.mergedadmin.openapi.SwaggerConfig}.
 */
@Configuration
public class AdminSwaggerConfig {

	private final OpenApiProperties openApiProperties;

	@Value("${openapi.group.masterdata.name:Admin Master Service}")
	private String masterdataGroupName;

	public AdminSwaggerConfig(OpenApiProperties openApiProperties) {
		this.openApiProperties = openApiProperties;
	}

	/**
	 * admin-service's group. Masterdata's endpoints are also served under
	 * {@link AdminMasterdataPathConfig#ADMIN_MASTERDATA_PREFIX}; they are excluded here so they are
	 * documented once, in masterdata's own group, and the group description says where they are.
	 */
	@Bean
	public GroupedOpenApi groupedOpenApi() {
		return GroupedOpenApi.builder().group(openApiProperties.groupName())
				.pathsToMatch(openApiProperties.groupPaths().toArray(String[]::new))
				.pathsToExclude(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/**")
				.addOpenApiCustomizer(openApi -> openApi.setInfo(withMasterdataNote(openApi.getInfo())))
				.build();
	}

	/**
	 * Swagger UI renders the description as Markdown: the blank line starts the note as its own
	 * paragraph, and the backticks keep the paths' {@code **} from being read as bold markers.
	 */
	private Info withMasterdataNote(Info info) {
		String description = info.getDescription() == null ? "" : info.getDescription() + "\n\n";
		return new Info().title(info.getTitle()).version(info.getVersion()).license(info.getLicense())
				.contact(info.getContact()).termsOfService(info.getTermsOfService())
				.description(description + "All masterdata endpoints are also served under `"
						+ AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/**`, with the same requests and "
						+ "responses as under `" + ApiPathPrefixConfig.MASTERDATA_PREFIX + "/**`; they are documented "
						+ "in the '" + masterdataGroupName + "' group.");
	}
}
