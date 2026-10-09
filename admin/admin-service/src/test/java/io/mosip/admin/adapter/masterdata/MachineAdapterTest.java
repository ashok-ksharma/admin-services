package io.mosip.admin.adapter.masterdata;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.mosip.kernel.masterdata.dto.MachineRegistrationCenterDto;
import io.mosip.kernel.masterdata.dto.PageDto;
import io.mosip.kernel.masterdata.exception.MasterDataServiceException;
import io.mosip.kernel.masterdata.exception.RequestException;
import io.mosip.kernel.masterdata.service.MachineService;

@RunWith(MockitoJUnitRunner.class)
public class MachineAdapterTest {

	@Mock
	private MachineService machineService;

	@InjectMocks
	private MachineAdapter machineAdapter;

	@Test
	public void returnsTheMachinesInTheOrderMasterdataReturnsThem() {
		Mockito.when(machineService.getMachinesByRegistrationCenter("10001", 0, Integer.MAX_VALUE, "cr_dtimes", "DESC"))
				.thenReturn(page("10001", "2222", "1111"));

		List<MachineRegistrationCenterDto> machines = machineAdapter.getMachinesMappedToCenter("10001");

		assertEquals(2, machines.size());
		assertEquals("2222", machines.get(0).getId());
		assertEquals("1111", machines.get(1).getId());
		Mockito.verify(machineService, Mockito.times(1))
				.getMachinesByRegistrationCenter("10001", 0, Integer.MAX_VALUE, "cr_dtimes", "DESC");
	}

	@Test(expected = RequestException.class)
	public void noMachineMappedToTheCenterPropagatesToTheCaller() {
		Mockito.when(machineService.getMachinesByRegistrationCenter(Mockito.anyString(), Mockito.anyInt(),
				Mockito.anyInt(), Mockito.anyString(), Mockito.anyString()))
				.thenThrow(new RequestException("KER-MSD-030", "Machine not Found"));

		machineAdapter.getMachinesMappedToCenter("10001");
	}

	@Test(expected = MasterDataServiceException.class)
	public void otherFailuresPropagateToTheCaller() {
		Mockito.when(machineService.getMachinesByRegistrationCenter(Mockito.anyString(), Mockito.anyInt(),
				Mockito.anyInt(), Mockito.anyString(), Mockito.anyString()))
				.thenThrow(new MasterDataServiceException("KER-MSD-031", "Error while fetching machines"));

		machineAdapter.getMachinesMappedToCenter("10001");
	}

	private static PageDto<MachineRegistrationCenterDto> page(String centerId, String... machineIds) {
		List<MachineRegistrationCenterDto> machines = new ArrayList<>();
		for (String machineId : machineIds) {
			MachineRegistrationCenterDto machine = new MachineRegistrationCenterDto();
			machine.setId(machineId);
			machine.setRegCentId(centerId);
			machines.add(machine);
		}
		PageDto<MachineRegistrationCenterDto> pageDto = new PageDto<>();
		pageDto.setData(machines);
		return pageDto;
	}
}
