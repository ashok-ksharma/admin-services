package io.mosip.masterdata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.masterdata.TestBootApplication;
import io.mosip.masterdata.utils.MasterDataTest;
import io.mosip.masterdata.utils.AuditUtil;
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
		// No controller maps this request; masterdata's catch-all answers it with KER-MSD-500.
		MasterDataTest.checkUnmappedRequestResponse(mockMvc.perform(
				MasterDataTest.getOnMasterdataPath("/machines/10001/2011-12-10T17:39:48.765Z")).andReturn());
	}
	

}
