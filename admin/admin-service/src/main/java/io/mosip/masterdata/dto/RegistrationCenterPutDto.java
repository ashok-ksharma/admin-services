package io.mosip.masterdata.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class RegistrationCenterPutDto {

	@NotBlank
	private List<RegCenterPutReqDto> registrationCenterPutReqAdmDtos;

}
