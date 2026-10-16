package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.postresponse.PostLocationCodeResponseDto;
import lombok.Data;

@Data

public class LocationCodeResponseDto {

	List<PostLocationCodeResponseDto> locations;
}
