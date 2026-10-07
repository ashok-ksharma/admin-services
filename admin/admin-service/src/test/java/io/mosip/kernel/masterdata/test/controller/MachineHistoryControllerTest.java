package io.mosip.kernel.masterdata.test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.masterdata.test.TestBootApplication;
import io.mosip.kernel.masterdata.test.utils.MasterDataTest;
import io.mosip.kernel.masterdata.utils.AuditUtil;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestBootApplication.class)
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MachineHistoryControllerTest {
	
	@Autowired
	public MockMvc mockMvc;

	@MockBean
	private PublisherClient<String, EventModel, HttpHeaders> publisher;

	@MockBean
	private AuditUtil auditUtil;
	private ObjectMapper mapper;
	
	@Before
	public void setUp() {
		mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		doNothing().when(auditUtil).auditRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),Mockito.anyString());
	}
	
	@Test
	@WithUserDetails("global-admin")
	public void getMachineHistoryIdLangEffTest_WithEngLangCode() throws Exception {
		MasterDataTest.checkResponse(mockMvc.perform(MockMvcRequestBuilders.get("/machineshistories/10001/eng/2024-12-10T17:39:48.765Z")).andReturn(), null);

	}
	
	@Test
	@WithUserDetails("global-admin")
	public void getMachineHistoryIdLangEffTest_WithAraLangCode() throws Exception {
		MasterDataTest.checkResponse(mockMvc.perform(MockMvcRequestBuilders.get("/machineshistories/10001/ara/2024-12-10T17:39:48.765Z")).andReturn(), null);

	}

	@Test
	@WithUserDetails("global-admin")
	public void getMachineHistoryIdLangEffTest_WithOutLangCode() throws Exception {
		// Step 1b (admin-service merge): no masterdata controller maps this request, so Spring's
		// default resolver answers 404 (NoResourceFoundException). Before the merge masterdata's exception advice was
		// unscoped and its catch-all turned this into HTTP 500; it is now scoped to
		// io.mosip.kernel.masterdata and does not apply when no controller matches. Intended
		// behaviour change - revisit per admin-service-merge-plan.md Appendix D.2 if clients rely
		// on the old 500.
		mockMvc.perform(MockMvcRequestBuilders.get("/machines/10001/2011-12-10T17:39:48.765Z"))
				.andExpect(status().isNotFound());
	}
	

}
