package io.mosip.masterdata.service;

import io.mosip.masterdata.dto.DeviceProviderDto;
import io.mosip.masterdata.dto.ValidateDeviceDto;
import io.mosip.masterdata.dto.getresponse.ResponseDto;
import io.mosip.masterdata.dto.getresponse.extn.DeviceProviderExtnDto;

/**
 * 
 * @author Srinivasan
 * @author Megha Tanga
 *
 */

public interface DeviceProviderService {

	/**
	 * Validate device providers.
	 *
	 * @param validateDeviceDto the validate device dto
	 * @return {@link ResponseDto}
	 */
	ResponseDto validateDeviceProviders(ValidateDeviceDto validateDeviceDto);



	/**
	 * Method to create Device Provider
	 * 
	 * @param dto Device Provider dto from user
	 * @return DeviceProviderExtnDto device Provider dto which has created
	 */
	public DeviceProviderExtnDto createDeviceProvider(DeviceProviderDto dto);

	/**
	 * Method to update Device Provider
	 * 
	 * @param dto Device Provider dto from user
	 * @return DeviceProviderExtnDto device Provider dto which has updated
	 */
	public DeviceProviderExtnDto updateDeviceProvider(DeviceProviderDto dto);
}
