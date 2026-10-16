
package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.DeviceDto;
import lombok.Data;

@Data

public class DeviceResponseDto {
	private List<DeviceDto> devices;
}
