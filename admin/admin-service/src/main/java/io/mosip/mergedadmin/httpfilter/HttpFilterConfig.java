package io.mosip.mergedadmin.httpfilter;

import jakarta.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Request filters for the merged application, moved unchanged from admin-service's CommonConfig
 * (same bean names, same order).
 * <p>
 * Both are registered without a URL pattern, so they run for every request. Today that is right:
 * admin's and masterdata's response-body advices read the request body cached by
 * {@link ReqResFilter} (masterdata's own copy of these filters was removed at the merge). When
 * another service is merged and does not need them, register them for the admin and masterdata
 * URL prefixes only instead of every request.
 * </p>
 */
@Configuration
public class HttpFilterConfig {

	@Bean
	public CommonsRequestLoggingFilter logFilter() {
		CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
		filter.setIncludeQueryString(true);
		filter.setIncludePayload(true);
		filter.setMaxPayloadLength(10000);
		filter.setIncludeHeaders(false);
		filter.setAfterMessagePrefix("REQUEST DATA : ");
		return filter;
	}

	@Bean
	public FilterRegistrationBean<Filter> registerReqResFilter() {
		FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
		filterRegistrationBean.setFilter(getReqResFilter());
		filterRegistrationBean.setOrder(2);
		return filterRegistrationBean;
	}

	@Bean
	public Filter getReqResFilter() {
		return new ReqResFilter();
	}
}
