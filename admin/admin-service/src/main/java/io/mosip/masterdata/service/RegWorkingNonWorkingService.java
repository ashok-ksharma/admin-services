package io.mosip.masterdata.service;

import io.mosip.masterdata.dto.WeekDaysResponseDto;
import io.mosip.masterdata.dto.WorkingDaysResponseDto;
import io.mosip.masterdata.dto.getresponse.StatusResponseDto;
import io.mosip.masterdata.dto.getresponse.extn.WorkingDaysExtnDto;
import io.mosip.masterdata.dto.request.WorkingDaysPutRequestDto;

public interface RegWorkingNonWorkingService {

	WeekDaysResponseDto getWeekDaysList(String regCenterId, String langCode);

	WorkingDaysResponseDto getWorkingDays(String regCenterId, String dayCode);
	
	WorkingDaysResponseDto getWorkingDays(String langCode);

	WorkingDaysExtnDto updateWorkingDays(WorkingDaysPutRequestDto request) throws NoSuchFieldException, IllegalAccessException;

	StatusResponseDto updateWorkingDaysStatus(String code, boolean isActive);
}
