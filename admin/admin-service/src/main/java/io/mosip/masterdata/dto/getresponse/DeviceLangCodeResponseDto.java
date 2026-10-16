
package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.DeviceLangCodeDtypeDto;
import lombok.Data;

@Data

public class DeviceLangCodeResponseDto {
	List<DeviceLangCodeDtypeDto> devices;
}
