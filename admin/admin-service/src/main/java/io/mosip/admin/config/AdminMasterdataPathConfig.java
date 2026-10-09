package io.mosip.admin.config;

import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import io.mosip.admin.packetstatusupdater.util.AuditUtil;
import io.mosip.admin.packetstatusupdater.util.EventEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Serves every masterdata endpoint under {@value #ADMIN_MASTERDATA_PREFIX} as well as under
 * {@link ApiPathPrefixConfig#MASTERDATA_PREFIX}.
 *
 * <p>
 * admin-service has always exposed masterdata's API under {@code /v1/admin/masterdata/**}
 * (admin UI calls it there). Each masterdata controller method is registered a second time
 * with that prefix, so both paths are handled directly by masterdata's controllers, with the
 * same request handling, security, exception handling and responses. The second path is
 * registered on the application's request mapping itself, so it gets the same interceptors
 * and CORS settings as every other path.
 * </p>
 *
 * <p>
 * Requests on {@value #ADMIN_MASTERDATA_PREFIX} are audited as
 * {@link EventEnum#MASTERDATA_PROXY_API_CALLED}. The second path is excluded from admin's
 * Swagger group (see {@link SwaggerConfig}) so masterdata's endpoints are documented once.
 * </p>
 */
@Configuration
public class AdminMasterdataPathConfig implements WebMvcConfigurer {

	public static final String ADMIN_MASTERDATA_PREFIX = ApiPathPrefixConfig.ADMIN_PREFIX + "/masterdata";

	private static final String MASTERDATA_PACKAGE = "io.mosip.kernel.masterdata.";

	@Autowired
	private AuditUtil auditUtil;

	@Bean
	public WebMvcRegistrations adminMasterdataWebMvcRegistrations() {
		return new WebMvcRegistrations() {
			@Override
			public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
				return new AdminMasterdataRequestMappingHandlerMapping();
			}
		};
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new HandlerInterceptor() {
			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
				auditUtil.setAuditRequestDto(EventEnum.MASTERDATA_PROXY_API_CALLED, null);
				return true;
			}
		}).addPathPatterns(ADMIN_MASTERDATA_PREFIX + "/**");
	}

	/**
	 * Registers each masterdata controller method a second time, with its
	 * {@code /v1/masterdata} prefix replaced by {@value AdminMasterdataPathConfig#ADMIN_MASTERDATA_PREFIX}.
	 */
	static class AdminMasterdataRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

		@Override
		protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
			super.registerHandlerMethod(handler, method, mapping);
			if (isMasterdataHandler(handler) && mapping.getPatternValues().stream()
					.allMatch(pattern -> pattern.startsWith(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/"))) {
				super.registerHandlerMethod(handler, method, withAdminMasterdataPrefix(mapping));
			}
		}

		private boolean isMasterdataHandler(Object handler) {
			Class<?> type = handler instanceof String ? obtainApplicationContext().getType((String) handler)
					: handler.getClass();
			return type != null && ClassUtils.getUserClass(type).getName().startsWith(MASTERDATA_PACKAGE);
		}

		private RequestMappingInfo withAdminMasterdataPrefix(RequestMappingInfo mapping) {
			Set<String> patterns = mapping.getPatternValues();
			String[] aliases = patterns.stream()
					.map(pattern -> ADMIN_MASTERDATA_PREFIX + pattern.substring(ApiPathPrefixConfig.MASTERDATA_PREFIX.length()))
					.toArray(String[]::new);
			return mapping.mutate().paths(aliases).options(getBuilderConfiguration()).build();
		}
	}
}
