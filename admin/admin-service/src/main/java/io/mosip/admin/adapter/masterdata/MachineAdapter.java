package io.mosip.admin.adapter.masterdata;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.mosip.kernel.masterdata.dto.MachineRegistrationCenterDto;
import io.mosip.kernel.masterdata.service.MachineService;

/**
 * Adapter for masterdata's {@link MachineService}.
 */
@Component
public class MachineAdapter {

	private static final String ORDER_BY_CREATED = "cr_dtimes";
	private static final String NEWEST_FIRST = "DESC";

	@Autowired
	private MachineService machineService;

	/**
	 * All machines mapped to the given registration centre, newest first
	 * ({@code cr_dtimes DESC}), fetched in a single call. masterdata's errors - including
	 * "Machine not Found" (KER-MSD-030) when no machine is mapped - propagate unchanged.
	 *
	 * @param centerId registration centre ID
	 * @return the machines mapped to the centre
	 */
	public List<MachineRegistrationCenterDto> getMachinesMappedToCenter(String centerId) {
		return machineService
				.getMachinesByRegistrationCenter(centerId, 0, Integer.MAX_VALUE, ORDER_BY_CREATED, NEWEST_FIRST)
				.getData();
	}
}
