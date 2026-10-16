package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.IdTypeDto;
import lombok.Data;

/**
 * DTO class for id types response.
 * 
 * @author Sagar Mahapatra
 * @since 1.0.0
 *
 */
@Data
public class IdTypeResponseDto {
	/**
	 * List of id types.
	 */
	private List<IdTypeDto> idtypes;
}
