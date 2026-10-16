package io.mosip.masterdata.dto;

import io.mosip.masterdata.dto.getresponse.extn.BaseDto;
import lombok.Data;
@Data
public class ZoneUserExtnDto extends BaseDto {
	
	private String zoneCode;

	
	private String userId;

	private String langCode;
}
