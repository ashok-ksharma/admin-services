package io.mosip.masterdata.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.mosip.mergedadmin.web.ApiPathPrefixConfig;

/**
 * The Class AuditConfigurer.
 *
 * @author Srinivasan
 */
@Configuration
//@EnableWebMvc
public class AuditConfigurer implements WebMvcConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(AuditConfigurer.class);

	/**
	 * Auditable end points.
	 *
	 * @return the string[]
	 */
	private String[] auditableEndPoints() {
		return new String[] { "http://localhost:8086/v1/masterdata/devices/search" };

	}

	@Autowired
	private AuditInterceptor auditInterceptor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurer#
	 * addInterceptors(org.springframework.web.servlet.config.annotation.
	 * InterceptorRegistry)
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		logger.debug("add Intercpetors called");
		// Before the merge masterdata ran with context path /v1/masterdata, so "/**/search" matched
		// only masterdata's search URLs (admin's /v1/admin/masterdata proxy forwarded to them too).
		// In the merged application it would match every service's, so it is limited to masterdata.
		registry.addInterceptor(auditInterceptor).addPathPatterns(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/**/search",
				ApiPathPrefixConfig.ADMIN_PREFIX + "/masterdata/**/search");
	}
}
