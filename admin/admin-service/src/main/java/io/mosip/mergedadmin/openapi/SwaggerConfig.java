package io.mosip.mergedadmin.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuration class for swagger config
 * <p>
 * The application's single OpenAPI description and Swagger UI. Each merged service contributes its
 * own Swagger group from its own package (admin: {@code AdminSwaggerConfig}, masterdata:
 * {@code MasterDataSwaggerConfig}); springdoc picks up every {@code GroupedOpenApi} bean, so a
 * merged service adds its group without editing this class.
 * </p>
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

}
