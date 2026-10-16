package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.MachineDto;
import lombok.Data;

/**
 * 
 * @author Megha Tanga
 * @version 1.0.0
 */
@Data

public class MachineResponseDto {
	private List<MachineDto> machines;
}
