package io.mosip.masterdata.service;

import io.mosip.masterdata.dto.getresponse.ExceptionalHolidayResponseDto;
import io.mosip.masterdata.dto.getresponse.HolidayResponseDto;
import io.mosip.masterdata.exception.DataNotFoundException;
import io.mosip.masterdata.exception.MasterDataServiceException;

/**
 * @author Kishan Rathore
 *
 */
public interface ExceptionalHolidayService {

	/**
	 * To fetch all the holidays
	 * 
	 * @return {@linkplain HolidayResponseDto}
	 * @throws MasterDataServiceException when data not fetched from DB
	 * @throws DataNotFoundException      when data not found
	 */
	ExceptionalHolidayResponseDto getAllExceptionalHolidays(String regCenterId, String langCode);

	ExceptionalHolidayResponseDto getAllExceptionalHolidays(String regCenterId);

}
