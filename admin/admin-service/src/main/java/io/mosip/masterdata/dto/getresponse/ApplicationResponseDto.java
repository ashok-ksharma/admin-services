package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.ApplicationDto;
import lombok.Data;

@Data

public class ApplicationResponseDto {

	private List<ApplicationDto> applicationtypes;

}
