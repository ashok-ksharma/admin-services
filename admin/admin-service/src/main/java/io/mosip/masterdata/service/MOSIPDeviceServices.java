package io.mosip.masterdata.service;

import io.mosip.masterdata.dto.MOSIPDeviceServiceDto;
import io.mosip.masterdata.dto.MOSIPDeviceServiceExtDto;
import io.mosip.masterdata.dto.MOSIPDeviceServicePUTDto;

/**
 * 
 * @author Megha Tanga
 *
 */

public interface MOSIPDeviceServices {

	public MOSIPDeviceServiceExtDto createMOSIPDeviceService(MOSIPDeviceServiceDto dto);

	public MOSIPDeviceServiceExtDto updateMOSIPDeviceService(MOSIPDeviceServicePUTDto dto);

}
