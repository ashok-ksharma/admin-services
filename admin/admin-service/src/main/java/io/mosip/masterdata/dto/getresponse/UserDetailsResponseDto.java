package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.UserDetailsDto;
import lombok.Data;

/**
 * 
 * @author Megha Tanga
 * @version 1.0.0
 */
@Data

public class UserDetailsResponseDto {
	private List<UserDetailsDto> machines;
}
