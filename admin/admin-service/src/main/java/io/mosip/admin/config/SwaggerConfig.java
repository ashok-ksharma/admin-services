package io.mosip.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuration class for swagger config
 * 
 * @author Srinivasan
 * @since 1.0.0
 * @author Govindaraj Velu
 * @implSpec upgrade the Swagger2.0 to OpenAPI (Swagger3.0)
 *
 */
@Configuration
public class SwaggerConfig {
	
	private static final Logger logger = LoggerFactory.getLogger(SwaggerConfig.class);

	@Autowired
	private OpenApiProperties openApiProperties;

	@Value("${openapi.group.masterdata.name:Admin Master Service}")
	private String masterdataGroupName;
	
	@Bean
    public OpenAPI openApi() {
		OpenAPI api = new OpenAPI()
                .components(new Components())
                .info(new Info()
                		.title(openApiProperties.getInfo().getTitle())
                		.version(openApiProperties.getInfo().getVersion())
                		.description(openApiProperties.getInfo().getDescription())
                		.license(new License()
                				.name(openApiProperties.getInfo().getLicense().getName())
                				.url(openApiProperties.getInfo().getLicense().getUrl())));
			
			openApiProperties.getService().getServers().forEach(server -> {
				api.addServersItem(new Server().description(server.getDescription()).url(server.getUrl()));
			});
			logger.info("swagger open api bean is ready");
		return api;
    }
	
	/**
	 * admin-service's group. Masterdata's endpoints are also served under
	 * {@link AdminMasterdataPathConfig#ADMIN_MASTERDATA_PREFIX}; they are excluded here so they are
	 * documented once, in masterdata's own group, and the group description says where they are.
	 */
	@Bean
	public GroupedOpenApi groupedOpenApi() {
		return GroupedOpenApi.builder().group(openApiProperties.getGroup().getName())
				.pathsToMatch(openApiProperties.getGroup().getPaths().stream().toArray(String[]::new))
				.pathsToExclude(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/**")
				.addOpenApiCustomizer(openApi -> openApi.setInfo(withMasterdataNote(openApi.getInfo())))
				.build();
	}

	private Info withMasterdataNote(Info info) {
		String description = info.getDescription() == null ? "" : info.getDescription() + " ";
		return new Info().title(info.getTitle()).version(info.getVersion()).license(info.getLicense())
				.contact(info.getContact()).termsOfService(info.getTermsOfService())
				.description(description + "All masterdata endpoints are also served under "
						+ AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/**, with the same requests and "
						+ "responses as under " + ApiPathPrefixConfig.MASTERDATA_PREFIX + "/**; they are documented "
						+ "in the '" + masterdataGroupName + "' group.");
	}
	
}
