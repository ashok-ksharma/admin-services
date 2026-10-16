package io.mosip.admin.config;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.admin.packetstatusupdater.exception.ApiExceptionHandler;
import io.mosip.mergedadmin.module.ServiceModule;
import io.mosip.mergedadmin.web.ApiPathPrefixConfig;

/**
 * admin-service's declaration for the merged application's web layer.
 * <p>
 * {@code io.mosip.kernel.authcodeflowproxy} is listed with admin's packages: its
 * {@code LoginController} (the admin UI's login) has no class-level {@code @RequestMapping}, so
 * {@code /login/**}, {@code /login-redirect/**}, {@code /logout/user} and
 * {@code /authorize/admin/validateToken} were served under {@code /v1/admin} by the old
 * context-path. Omitting the package would move those endpoints to the server root.
 * </p>
 * <p>
 * Unmapped paths under {@code /v1/admin} (including {@code /v1/admin/actuator/...} and the API
 * documentation, which sat under admin's context-path) are answered by admin's catch-all, as
 * before the merge. {@code /v1/admin/masterdata} is answered by masterdata (see its declaration).
 * </p>
 */
@Configuration
public class AdminServiceModuleConfig {

	@Bean
	public ServiceModule adminServiceModule(ObjectProvider<ApiExceptionHandler> exceptionHandler) {
		return new ServiceModule("admin", ApiPathPrefixConfig.ADMIN_PREFIX,
				List.of("io.mosip.admin", "io.mosip.kernel.authcodeflowproxy"), List.of(ApiPathPrefixConfig.ADMIN_PREFIX),
				(request, e) -> {
					ApiExceptionHandler handler = exceptionHandler.getIfAvailable();
					return handler == null ? null : handler.defaultErrorHandler(request, e);
				});
	}
}
