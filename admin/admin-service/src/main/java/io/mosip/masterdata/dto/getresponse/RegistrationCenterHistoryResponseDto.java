package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.RegistrationCenterHistoryDto;
import lombok.Data;

/**
 * 
 * @author Abhishek Kumar
 * @version 1.0.0
 * @since 23-10-2016
 */
@Data
public class RegistrationCenterHistoryResponseDto {
	private List<RegistrationCenterHistoryDto> registrationCentersHistory;
}
