package io.mosip.masterdata.config;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.mosip.masterdata.exception.MasterDataApiExceptionHandler;
import io.mosip.mergedadmin.module.ServiceModule;
import io.mosip.mergedadmin.web.ApiPathPrefixConfig;

/**
 * masterdata's declaration for the merged application's web layer.
 * <p>
 * Every masterdata endpoint is also served under {@code /v1/admin/masterdata} (admin's
 * {@code AdminMasterdataPathConfig}), so unmapped paths there are answered by masterdata's
 * catch-all too, as masterdata answered them behind admin's proxy before the merge.
 * </p>
 */
@Configuration
public class MasterdataServiceModuleConfig {

	@Bean
	public ServiceModule masterdataServiceModule(ObjectProvider<MasterDataApiExceptionHandler> exceptionHandler) {
		return new ServiceModule("masterdata", ApiPathPrefixConfig.MASTERDATA_PREFIX, List.of("io.mosip.masterdata"),
				List.of(ApiPathPrefixConfig.MASTERDATA_PREFIX, ApiPathPrefixConfig.ADMIN_PREFIX + "/masterdata"),
				(request, e) -> {
					MasterDataApiExceptionHandler handler = exceptionHandler.getIfAvailable();
					return handler == null ? null : handler.defaultErrorHandler(request, e);
				});
	}
}
