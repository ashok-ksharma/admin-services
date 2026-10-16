package io.mosip.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The request filters this class used to register (logFilter, registerReqResFilter,
 * getReqResFilter) serve every merged service and are in
 * {@link io.mosip.mergedadmin.httpfilter.HttpFilterConfig}.
 *
 * @author Srinivasan
 */
@Configuration
public class CommonConfig {

	@Bean
	public Properties packetProperties() {
		Properties properties = new Properties();
		try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream("globalMessages.properties")) {
			properties.load(inputStream);
		} catch (IOException e) { }
		return properties;
	}
}
