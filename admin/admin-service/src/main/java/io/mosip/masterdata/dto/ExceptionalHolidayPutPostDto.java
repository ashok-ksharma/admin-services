
package io.mosip.masterdata.dto;

import io.mosip.masterdata.validator.ValidDateFormat;
import lombok.Data;

@Data
public class ExceptionalHolidayPutPostDto {

	@ValidDateFormat(message = "Invalid Date Format")
	private String exceptionHolidayDate;

	private String exceptionHolidayName;

	private String exceptionHolidayReson;

}
