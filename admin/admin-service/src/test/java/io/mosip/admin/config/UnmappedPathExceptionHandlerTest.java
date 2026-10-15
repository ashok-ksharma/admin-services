package io.mosip.admin.config;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.mosip.admin.packetstatusupdater.exception.ApiExceptionHandler;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.masterdata.exception.MasterDataApiExceptionHandler;

/**
 * Routing of {@link UnmappedPathExceptionHandler}: each served prefix goes to that
 * service's own catch-all, everything else is rethrown so Spring answers 404. The responses
 * themselves are checked end to end in {@code FullApplicationContextTest}.
 */
public class UnmappedPathExceptionHandlerTest {

	private final ResponseEntity<ResponseWrapper<ServiceError>> adminResponse = new ResponseEntity<>(
			HttpStatus.INTERNAL_SERVER_ERROR);

	private final ResponseEntity<ResponseWrapper<ServiceError>> masterDataResponse = new ResponseEntity<>(
			HttpStatus.INTERNAL_SERVER_ERROR);

	private ApiExceptionHandler adminHandler;

	private MasterDataApiExceptionHandler masterDataHandler;

	private UnmappedPathExceptionHandler handler;

	@Before
	public void setUp() throws Exception {
		adminHandler = mock(ApiExceptionHandler.class);
		masterDataHandler = mock(MasterDataApiExceptionHandler.class);
		when(adminHandler.defaultErrorHandler(any(), any())).thenReturn(adminResponse);
		when(masterDataHandler.defaultErrorHandler(any(), any())).thenReturn(masterDataResponse);
		handler = new UnmappedPathExceptionHandler(provider(adminHandler), provider(masterDataHandler));
	}

	@Test
	public void masterdataPrefixGoesToMasterdataHandler() throws Exception {
		assertSame(masterDataResponse, handle("/v1/masterdata/registrationcenters/page/eng/5/"));
		verify(adminHandler, never()).defaultErrorHandler(any(), any());
	}

	@Test
	public void adminMasterdataPrefixGoesToMasterdataHandler() throws Exception {
		assertSame(masterDataResponse, handle("/v1/admin/masterdata/no-such-path"));
		verify(adminHandler, never()).defaultErrorHandler(any(), any());
	}

	@Test
	public void adminPrefixGoesToAdminHandler() throws Exception {
		assertSame(adminResponse, handle("/v1/admin/no-such-path"));
		verify(masterDataHandler, never()).defaultErrorHandler(any(), any());
	}

	@Test
	public void otherPathsAreRethrown() throws Exception {
		for (String path : new String[] { "/v1/hotlist/no-such-path", "/v1/syncdata/no-such-path", "/no-such-root",
				"/v1/masterdataextra/x", "/v1/adminextra/x" }) {
			assertRethrown(path);
		}
		verify(adminHandler, never()).defaultErrorHandler(any(), any());
		verify(masterDataHandler, never()).defaultErrorHandler(any(), any());
	}

	@Test
	public void missingServiceHandlerIsRethrown() throws Exception {
		handler = new UnmappedPathExceptionHandler(provider(null), provider(null));
		assertRethrown("/v1/masterdata/no-such-path");
		assertRethrown("/v1/admin/no-such-path");
	}

	private ResponseEntity<ResponseWrapper<ServiceError>> handle(String path) throws Exception {
		return handler.handleUnmappedRequest(new MockHttpServletRequest("GET", path), exception(path));
	}

	private void assertRethrown(String path) throws Exception {
		NoResourceFoundException e = exception(path);
		try {
			handler.handleUnmappedRequest(new MockHttpServletRequest("GET", path), e);
			fail("Expected " + path + " to be rethrown");
		} catch (NoResourceFoundException thrown) {
			assertSame(e, thrown);
		}
	}

	private static NoResourceFoundException exception(String path) {
		return new NoResourceFoundException(HttpMethod.GET, path.substring(1));
	}

	@SuppressWarnings("unchecked")
	private static <T> ObjectProvider<T> provider(T bean) {
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(bean);
		return provider;
	}
}
