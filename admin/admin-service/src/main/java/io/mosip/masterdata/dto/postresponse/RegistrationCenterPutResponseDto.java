package io.mosip.masterdata.dto.postresponse;

import java.util.List;

import io.mosip.masterdata.dto.RegCenterPutReqDto;
import io.mosip.masterdata.dto.getresponse.extn.RegistrationCenterExtnDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This response DTO for update Registration center by Admin
 * 
 * @author Megha Tanga
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationCenterPutResponseDto {
	private List<RegistrationCenterExtnDto> registrationCenters;
	private List<RegCenterPutReqDto> notUpdatedRegistrationCenters;
	private List<RegistrationCenterExtnDto> newRegistartionCenters;

}
