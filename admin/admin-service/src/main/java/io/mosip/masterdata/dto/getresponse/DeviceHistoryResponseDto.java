package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.DeviceHistoryDto;
import lombok.Data;

/**
 * 
 * @author Megha Tanga
 * @version 1.0.0
 */
@Data
public class DeviceHistoryResponseDto {
	private List<DeviceHistoryDto> deviceHistoryDetails;
}
