package io.mosip.mergedadmin.openapi;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author GOVINDARAJ VELU
 *
 */
@Configuration
@ConfigurationProperties(prefix = "openapi")
@Data
public class OpenApiProperties {
	private InfoProperty info;
	private Service service;
	private Group group;

	/**
	 * {@code openapi.group.name} - admin's Swagger group, read by {@code AdminSwaggerConfig}
	 * outside this package (the {@code Group} type is package-private).
	 */
	public String groupName() {
		return group == null ? null : group.getName();
	}

	/** {@code openapi.group.paths} - see {@link #groupName()}. */
	public List<String> groupPaths() {
		return group == null || group.getPaths() == null ? List.of() : group.getPaths();
	}
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class InfoProperty {
	private String title;
	private String description;
	private String version;
	private LicenseProperty license;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class LicenseProperty {
	private String name;
	private String url;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class Service {
	private List<Server> servers;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class Server {
	private String description;
	private String url;
}

/**
 * @author GOVINDARAJ VELU
 *
 */
@Data
class Group {
	private String name;
	private List<String> paths;
}