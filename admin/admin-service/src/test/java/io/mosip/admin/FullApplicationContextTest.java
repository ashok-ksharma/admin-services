package io.mosip.admin;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.DataSource;
import javax.validation.Validator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.admin.adapter.masterdata.MachineAdapter;
import io.mosip.admin.bulkdataupload.service.PacketUploadService;
import io.mosip.admin.config.AdminMasterdataPathConfig;
import io.mosip.admin.config.ApiPathPrefixConfig;
import io.mosip.admin.packetstatusupdater.util.EventEnum;
import io.mosip.commons.packet.impl.OnlinePacketCryptoServiceImpl;
import io.mosip.commons.packet.keeper.PacketKeeper;
import io.mosip.kernel.authcodeflowproxy.api.config.AuthCodeProxyConfig;
import io.mosip.kernel.datamapper.orika.impl.DataMapperImpl;
import io.mosip.kernel.masterdata.service.MachineService;
import jakarta.persistence.EntityManagerFactory;

/**
 * Application-wide integration test: boots the <em>whole</em> admin-service application -
 * admin-service's own code together with kernel-masterdata-service's, exactly as
 * {@link AdminBootApplication} scans them - and checks the places where the two services
 * meet.
 *
 * <p>
 * <b>Why it exists.</b> Each service keeps its own, narrower test context: admin's tests
 * boot {@link TestBootApplication} (admin packages only, with masterdata beans that admin
 * code uses mocked) and masterdata's tests boot masterdata's own test application. That
 * keeps every service's tests fast and independent, but none of them proves that the
 * combined application wires together. This class is the single place that does: the real
 * beans of both services in one context, with the real web layer, security, exception
 * handling and persistence setup.
 * </p>
 *
 * <p>
 * <b>What it checks.</b>
 * </p>
 * <ol>
 * <li>The context starts and the beans that cross the service boundary exist.</li>
 * <li>There is exactly one {@code DataSource}, entity manager factory and transaction
 * manager.</li>
 * <li>Every controller is served under its own service's prefix: admin's under
 * {@code /v1/admin}, masterdata's under {@code /v1/masterdata}.</li>
 * <li>Actuator and the API documentation stay under {@code /v1/admin}.</li>
 * <li>Each Swagger group documents only its own service's endpoints.</li>
 * <li>Each service's errors keep that service's error shape; unmapped requests get the
 * standard 404/405.</li>
 * <li>Lazy associations can be read while the response is built (open-session-in-view on
 * the services that use it).</li>
 * <li>Masterdata's endpoints are also served under {@code /v1/admin/masterdata} with the same
 * responses, errors included.</li>
 * <li>Requests under {@code /v1/admin/masterdata} are audited.</li>
 * </ol>
 *
 * <p>
 * <b>What it deliberately does not do.</b> Business logic is covered by each service's own
 * tests and is not repeated here. Nothing outside the application is called: the H2 test
 * database replaces PostgreSQL, {@code TestSecurityConfig} users replace the auth adapter
 * (which is not on the test classpath), and the config-server download and audit calls are
 * stubbed. Problems that need real infrastructure are left to the API test rig.
 * </p>
 *
 * <p>
 * <b>Extending it.</b> Add a test here only for behaviour that depends on the services
 * running together - a new service combined into the application, a path served on behalf
 * of another service, cross-service wiring. Keep the boot configuration below in step with
 * {@link AdminBootApplication}'s component scan.
 * </p>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FullApplicationContextTest.FullApplication.class)
@AutoConfigureMockMvc
public class FullApplicationContextTest {

	/**
	 * The application as {@link AdminBootApplication} assembles it. Differences, all
	 * test-only: the auth adapter packages are not scanned (the adapter is kept off the test
	 * classpath, {@code TestSecurityConfig} stands in for it); masterdata's own test classes
	 * and admin's {@code TestConfig} are excluded (their {@code RestTemplate} beans are
	 * replaced below); {@code AuthCodeProxyConfig} is excluded because its only bean is a
	 * fallback {@code restTemplate} ({@code @ConditionalOnMissingBean}) that never applies in
	 * the application, where the auth adapter registers {@code restTemplate} first; external
	 * collaborators are mocked or stubbed as in admin's {@link TestBootApplication}.
	 */
	@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
			DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
	@ComponentScan(value = { "io.mosip.admin.*", "io.mosip.commons.*", "io.mosip.kernel.idvalidator.rid.*",
			"io.mosip.kernel.biometrics.*", "io.mosip.kernel.authcodeflowproxy.*",
			"io.mosip.kernel.masterdata.*", "io.mosip.kernel.core.datamapper.*", "io.mosip.kernel.core.websub.*",
			"io.mosip.kernel.idgenerator.*", "io.mosip.kernel.websub.api.*", "io.mosip.kernel.applicanttype.*",
			"io.mosip.kernel.core.idgenerator.*", "io.mosip.kernel.core.logger.config" },
			excludeFilters = {
					@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { DataMapperImpl.class }),
					@ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = "io.mosip.kernel.lkeymanager.repository.*"),
					@ComponentScan.Filter(type = FilterType.ASPECTJ, pattern = "io.mosip.kernel.masterdata.test..*"),
					@ComponentScan.Filter(type = FilterType.ASPECTJ,
							pattern = "io.mosip.admin.packetstatusupdater.config.TestConfig"),
					@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { AuthCodeProxyConfig.class }) })
	static class FullApplication {

		/**
		 * kernel-applicanttype's ApplicantTypeImpl downloads the applicant-type MVEL script
		 * from the config server when it starts; serve it instead of calling a real server.
		 */
		@Bean
		public RestTemplate restTemplate() {
			RestTemplate restTemplate = new RestTemplate();
			MockRestServiceServer.createServer(restTemplate)
					.expect(requestTo(endsWith("/applicanttype.mvel")))
					.andExpect(method(HttpMethod.GET))
					.andRespond(withSuccess().body("def getApplicantType() { return '000'; }"));
			return restTemplate;
		}

		@Bean
		public RestTemplate selfTokenRestTemplate() {
			return new RestTemplate();
		}

		@Bean
		@Primary
		public OnlinePacketCryptoServiceImpl onlineCrypto() {
			return Mockito.mock(OnlinePacketCryptoServiceImpl.class);
		}

		@Bean
		@Primary
		public PacketKeeper packetKeeper() {
			return Mockito.mock(PacketKeeper.class);
		}

		@Bean
		public Validator validator() {
			return Mockito.mock(Validator.class);
		}
	}

	private static final String MASTERDATA_PACKAGE = "io.mosip.kernel.masterdata.";

	/** Both services audit through the audit manager; keep those calls inside the test. */
	@MockBean
	private io.mosip.admin.packetstatusupdater.util.AuditUtil adminAuditUtil;

	@MockBean
	private io.mosip.kernel.masterdata.utils.AuditUtil masterdataAuditUtil;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	@Autowired
	private WebEndpointProperties webEndpointProperties;

	@Autowired
	private WebMvcEndpointHandlerMapping webMvcEndpointHandlerMapping;

	// 1 -----------------------------------------------------------------------------------

	@Test
	public void contextStartsWithTheBeansThatCrossTheServiceBoundary() {
		// admin code that uses masterdata, and masterdata code that uses admin
		assertNotNull(context.getBean(PacketUploadService.class));
		assertNotNull(context.getBean(MachineAdapter.class));
		assertNotNull(context.getBean(MachineService.class));
		assertNotNull(context.getBean(io.mosip.admin.packetstatusupdater.util.RestClient.class));
		// beans both services define, kept apart by name
		for (String name : List.of("auditUtil", "masterdataAuditUtil", "adminAuthorizedRoles", "authorizedRoles",
				"masterOpenEntityManagerInViewInterceptor")) {
			assertTrue("Expected bean '" + name + "' is missing", context.containsBean(name));
		}
		assertTrue(context.getBean("masterOpenEntityManagerInViewInterceptor") instanceof OpenEntityManagerInViewInterceptor);
	}

	// 2 -----------------------------------------------------------------------------------

	@Test
	public void thereIsExactlyOneDatasourceEntityManagerFactoryAndTransactionManager() {
		assertEquals(Set.of("masterDataSource"), context.getBeansOfType(DataSource.class).keySet());
		assertEquals(1, context.getBeansOfType(EntityManagerFactory.class).size());
		assertEquals(Set.of("masterTxManager"), context.getBeansOfType(PlatformTransactionManager.class).keySet());
	}

	// 3 -----------------------------------------------------------------------------------

	@Test
	public void everyControllerIsServedUnderItsOwnServicePrefix() {
		Set<String> wrongPrefix = new TreeSet<>();
		Set<String> patterns = new HashSet<>();
		int masterdataMappings = 0;
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : requestMappingHandlerMapping.getHandlerMethods()
				.entrySet()) {
			String beanType = entry.getValue().getBeanType().getName();
			if (beanType.startsWith("org.springframework")) {
				continue; // Boot's own /error mapping
			}
			boolean masterdata = beanType.startsWith(MASTERDATA_PACKAGE);
			if (masterdata) {
				masterdataMappings++;
			}
			for (String pattern : patternsOf(entry.getKey())) {
				patterns.add(pattern);
				boolean expected = masterdata
						? pattern.startsWith(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/")
								|| pattern.startsWith(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/")
						: pattern.startsWith(ApiPathPrefixConfig.ADMIN_PREFIX + "/")
								&& !pattern.startsWith(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/");
				if (!expected) {
					wrongPrefix.add(entry.getValue().getBeanType().getSimpleName() + " -> " + pattern);
				}
			}
		}
		assertTrue("No masterdata controllers were mapped", masterdataMappings > 0);
		assertTrue("Endpoints served outside their service prefix: " + wrongPrefix, wrongPrefix.isEmpty());
		// every masterdata endpoint is also served under /v1/admin/masterdata
		Set<String> missingAdminPath = new TreeSet<>();
		for (String pattern : patterns) {
			if (pattern.startsWith(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/")) {
				String adminPath = AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX
						+ pattern.substring(ApiPathPrefixConfig.MASTERDATA_PREFIX.length());
				if (!patterns.contains(adminPath)) {
					missingAdminPath.add(adminPath);
				}
			}
		}
		assertTrue("Masterdata endpoints missing under /v1/admin/masterdata: " + missingAdminPath,
				missingAdminPath.isEmpty());
		for (String expected : List.of("/v1/admin/roles", "/v1/admin/bulkupload", "/v1/admin/lostRid",
				"/v1/masterdata/languages", "/v1/masterdata/machines/mappedmachines/{regCenterId}")) {
			assertTrue("Expected mapping " + expected + " is missing", patterns.contains(expected));
		}
	}

	// 4 -----------------------------------------------------------------------------------

	@Test
	public void actuatorAndApiDocumentationStayUnderTheAdminPrefix() {
		assertEquals(ApiPathPrefixConfig.ADMIN_PREFIX + "/actuator", webEndpointProperties.getBasePath());
		Set<String> actuatorPatterns = new HashSet<>();
		webMvcEndpointHandlerMapping.getHandlerMethods().keySet()
				.forEach(info -> actuatorPatterns.addAll(info.getPatternValues()));
		assertFalse("No actuator endpoints were mapped", actuatorPatterns.isEmpty());
		assertTrue("Actuator endpoints outside /v1/admin/actuator: " + actuatorPatterns, actuatorPatterns.stream()
				.allMatch(p -> p.startsWith(ApiPathPrefixConfig.ADMIN_PREFIX + "/actuator")));

		Set<String> patterns = new HashSet<>();
		requestMappingHandlerMapping.getHandlerMethods().keySet().forEach(info -> patterns.addAll(patternsOf(info)));
		for (String expected : List.of("/v1/admin/swagger-ui.html", "/v1/admin/v3/api-docs")) {
			assertTrue("Expected springdoc mapping " + expected + " is missing", patterns.contains(expected));
		}
	}

	// 5 -----------------------------------------------------------------------------------

	@Test
	@WithUserDetails("global-admin")
	public void eachSwaggerGroupDocumentsOnlyItsOwnService() throws Exception {
		Map<String, GroupedOpenApi> groups = context.getBeansOfType(GroupedOpenApi.class);
		String masterdataGroup = groups.get("masterdataGroupedOpenApi").getGroup();
		String adminGroup = groups.get("groupedOpenApi").getGroup();

		Set<String> masterdataPaths = documentedPaths(masterdataGroup);
		assertFalse("The masterdata group documents no paths", masterdataPaths.isEmpty());
		assertTrue("Masterdata group documents non-masterdata paths: " + masterdataPaths,
				masterdataPaths.stream().allMatch(p -> p.startsWith(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/")));

		Set<String> adminPaths = documentedPaths(adminGroup);
		assertFalse("The admin group documents no paths", adminPaths.isEmpty());
		assertTrue("Admin group documents masterdata paths: " + adminPaths,
				adminPaths.stream().noneMatch(p -> p.startsWith(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/")
						|| p.startsWith(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/")));

		String note = "also served under " + AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX;
		assertTrue("Admin group description lacks the masterdata note",
				description(adminGroup).contains(note));
		assertFalse("Masterdata group description carries admin's note",
				description(masterdataGroup).contains(note));
	}

	// 6 -----------------------------------------------------------------------------------

	@Test
	@WithUserDetails("global-admin")
	public void eachServiceKeepsItsOwnErrorShape() throws Exception {
		// masterdata business error: masterdata's error codes
		mockMvc.perform(get("/v1/masterdata/packetrejectionreasons/NOSUCHCATEGORY/eng"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors[0].errorCode", org.hamcrest.Matchers.startsWith("KER-MSD-")));
		// admin business error: admin's error codes
		mockMvc.perform(get("/v1/admin/bulkupload/transcation/no-such-transaction"))
				.andExpect(jsonPath("$.errors[0].errorCode").value("ADMN-BLK-TRNSCTNS-001"));
		// no controller maps the request: Spring's standard responses
		mockMvc.perform(get("/v1/masterdata/no-such-path")).andExpect(status().isNotFound());
		mockMvc.perform(put("/v1/masterdata/devices/1000").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isMethodNotAllowed());
	}

	// 7 -----------------------------------------------------------------------------------

	@Test
	@WithUserDetails("global-admin")
	@Sql(statements = {
			"INSERT INTO master.reason_category (code, lang_code, name, descr, is_active, cr_by, cr_dtimes, is_deleted) "
					+ "VALUES ('FACTEST', 'eng', 'Full context test', 'Full context test', true, 'test', CURRENT_TIMESTAMP, false)",
			"INSERT INTO master.reason_list (rsncat_code, code, lang_code, name, descr, is_active, cr_by, cr_dtimes, is_deleted) "
					+ "VALUES ('FACTEST', 'R1', 'eng', 'Reason one', 'Reason one', true, 'test', CURRENT_TIMESTAMP, false)",
			"INSERT INTO master.reason_list (rsncat_code, code, lang_code, name, descr, is_active, cr_by, cr_dtimes, is_deleted) "
					+ "VALUES ('FACTEST', 'R2', 'eng', 'Reason two', 'Reason two', true, 'test', CURRENT_TIMESTAMP, false)" })
	@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, statements = {
			"DELETE FROM master.reason_list WHERE rsncat_code = 'FACTEST'",
			"DELETE FROM master.reason_category WHERE code = 'FACTEST'" })
	public void lazyAssociationsCanBeReadWhileTheResponseIsBuilt() throws Exception {
		// ReasonCategory.reasonList is lazy and is only read while mapping to the response,
		// after the repository call has returned - it needs the request-scoped session.
		mockMvc.perform(get("/v1/masterdata/packetrejectionreasons/FACTEST/eng"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errors").isEmpty())
				.andExpect(jsonPath("$.response.reasonCategories[0].reasonList.length()").value(2));
	}

	// 8 -----------------------------------------------------------------------------------

	@Test
	@WithUserDetails("global-admin")
	public void masterdataEndpointsAnswerTheSameUnderTheAdminPrefix() throws Exception {
		assertSameResponse("/languages", "global-admin");
	}

	// 9 -----------------------------------------------------------------------------------

	@Test
	@WithUserDetails("global-admin")
	public void masterdataErrorsAreTheSameUnderTheAdminPrefix() throws Exception {
		// business error
		assertSameResponse("/packetrejectionreasons/NOSUCHCATEGORY/eng", "global-admin");
		// no controller maps the request
		mockMvc.perform(get(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/no-such-path"))
				.andExpect(status().isNotFound());
		mockMvc.perform(put(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/devices/1000")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isMethodNotAllowed());
	}

	@Test
	@WithUserDetails("individual")
	public void masterdataRoleChecksAreTheSameUnderTheAdminPrefix() throws Exception {
		// "individual" lacks the roles this endpoint requires
		assertSameResponse("/packetrejectionreasons/MNA/eng", "individual");
	}

	// 10 ----------------------------------------------------------------------------------

	@Test
	@WithUserDetails("global-admin")
	public void requestsUnderTheAdminPrefixAreAudited() throws Exception {
		mockMvc.perform(get(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + "/languages"))
				.andExpect(status().isOk());
		Mockito.verify(adminAuditUtil, Mockito.times(1)).setAuditRequestDto(EventEnum.MASTERDATA_PROXY_API_CALLED, null);

		Mockito.clearInvocations(adminAuditUtil);
		mockMvc.perform(get(ApiPathPrefixConfig.MASTERDATA_PREFIX + "/languages")).andExpect(status().isOk());
		Mockito.verify(adminAuditUtil, Mockito.never()).setAuditRequestDto(EventEnum.MASTERDATA_PROXY_API_CALLED, null);
	}

	// -------------------------------------------------------------------------------------

	/**
	 * Same request on /v1/masterdata and /v1/admin/masterdata: same status, same body (the
	 * response timestamp aside).
	 */
	private void assertSameResponse(String path, String user) throws Exception {
		MvcResult direct = mockMvc.perform(get(ApiPathPrefixConfig.MASTERDATA_PREFIX + path)).andReturn();
		MvcResult viaAdmin = mockMvc.perform(get(AdminMasterdataPathConfig.ADMIN_MASTERDATA_PREFIX + path)).andReturn();
		assertEquals(path + " (" + user + "): status", direct.getResponse().getStatus(),
				viaAdmin.getResponse().getStatus());
		assertEquals(path + " (" + user + "): body", withoutTimestamp(direct), withoutTimestamp(viaAdmin));
	}

	private JsonNode withoutTimestamp(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		if (body.isBlank()) {
			return objectMapper.nullNode();
		}
		JsonNode json = objectMapper.readTree(body);
		if (json instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
			((com.fasterxml.jackson.databind.node.ObjectNode) json).remove(List.of("responsetime", "timestamp", "path"));
		}
		return json;
	}

	private String description(String group) throws Exception {
		String body = mockMvc.perform(get(ApiPathPrefixConfig.ADMIN_PREFIX + "/v3/api-docs/{group}", group))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).path("info").path("description").asText();
	}

	private Set<String> documentedPaths(String group) throws Exception {
		String body = mockMvc
				.perform(get(ApiPathPrefixConfig.ADMIN_PREFIX + "/v3/api-docs/{group}", group))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		JsonNode paths = objectMapper.readTree(body).path("paths");
		Set<String> result = new TreeSet<>();
		paths.fieldNames().forEachRemaining(result::add);
		return result;
	}

	private static Set<String> patternsOf(RequestMappingInfo info) {
		Set<String> patterns = new HashSet<>();
		if (info.getPathPatternsCondition() != null) {
			patterns.addAll(info.getPathPatternsCondition().getPatternValues());
		}
		if (info.getPatternsCondition() != null) {
			patterns.addAll(info.getPatternsCondition().getPatterns());
		}
		return patterns;
	}
}
