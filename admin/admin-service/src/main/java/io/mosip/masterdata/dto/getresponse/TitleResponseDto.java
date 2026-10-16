package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.TitleDto;
import lombok.Data;

/**
 * Dto class for fetching response from master data
 * 
 * @author Sidhant Agarwal
 * @since 1.0.0
 *
 */
@Data

public class TitleResponseDto {

	private List<TitleDto> titleList;

}
