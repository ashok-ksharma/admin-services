package io.mosip.kernel.masterdata.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseFilter;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.EmptyCheckUtils;

/*
 * Renamed from ResponseBodyAdviceConfig and scoped to masterdata's own controller
 * package during the admin-services merge. The rename resolves a bean-name collision
 * with io.mosip.admin.config.ResponseBodyAdviceConfig, whose body is identical to this
 * one; both were registering as "responseBodyAdviceConfig".
 *
 * The scope matters more here than it does on an exception advice. Only ONE exception
 * advice handles a given failure - Spring takes the first applicable one in @Order - but
 * EVERY applicable ResponseBodyAdvice runs, as a chain. Neither this bean nor
 * admin-service's declares an @Order, and both supports() methods return the same
 * condition (@ResponseFilter on the handler method), so without basePackages both would
 * fire on every admin-service response: the request body re-read, id and version re-set,
 * errors nulled a second time, and registerModule(new JavaTimeModule()) called twice per
 * request against the shared @Primary ObjectMapper.
 *
 * basePackages selects which CONTROLLERS an advice may serve - not where any class in the
 * response lives. Spring tests isApplicableToBeanType(handlerType) against the controller
 * that handled the request and drops advices whose packages do not contain it. So this
 * advice now wraps only masterdata's responses, and admin-service's - already scoped to
 * io.mosip.admin and io.mosip.kernel.authcodeflowproxy - only wraps its own.
 */
@RestControllerAdvice(basePackages = "io.mosip.kernel.masterdata")
public class MasterDataResponseBodyAdviceConfig implements ResponseBodyAdvice<ResponseWrapper<?>> {

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return returnType.hasMethodAnnotation(ResponseFilter.class);
	}

	@Override
	public ResponseWrapper<?> beforeBodyWrite(ResponseWrapper<?> body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {

		RequestWrapper<?> requestWrapper = null;
		String requestBody = null;

		try {
			HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();

			if (httpServletRequest instanceof ContentCachingRequestWrapper) {
				requestBody = new String(((ContentCachingRequestWrapper) httpServletRequest).getContentAsByteArray());
			} else if (httpServletRequest instanceof HttpServletRequestWrapper
					&& ((HttpServletRequestWrapper) httpServletRequest)
							.getRequest() instanceof ContentCachingRequestWrapper) {
				requestBody = new String(
						((ContentCachingRequestWrapper) ((HttpServletRequestWrapper) httpServletRequest).getRequest())
								.getContentAsByteArray());
			}

			objectMapper.registerModule(new JavaTimeModule());
			if (!EmptyCheckUtils.isNullEmpty(requestBody)) {
				requestWrapper = objectMapper.readValue(requestBody, RequestWrapper.class);
				body.setId(requestWrapper.getId());
				body.setVersion(requestWrapper.getVersion());
			}
			body.setErrors(null);
			return body;
		} catch (Exception e) {
			Logger mosipLogger = LoggerConfiguration.logConfig(MasterDataResponseBodyAdviceConfig.class);
			mosipLogger.error("", "", "", e.getMessage());
		}
		return body;
	}

}
