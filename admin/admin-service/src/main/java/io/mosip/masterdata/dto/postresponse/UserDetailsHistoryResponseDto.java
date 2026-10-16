package io.mosip.masterdata.dto.postresponse;

import java.util.List;

import io.mosip.masterdata.dto.UserDetailsDto;
import lombok.Data;

/**
 * @author Sidhant Agarwal
 * @since 1.0.0
 *
 */
@Data
public class UserDetailsHistoryResponseDto {

	private List<UserDetailsDto> userResponseDto;

}
