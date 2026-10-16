/**
 * 
 */
package io.mosip.masterdata.dto.registerdevice;

import jakarta.validation.constraints.Size;

import io.mosip.masterdata.validator.StringFormatter;
import io.mosip.masterdata.validator.registereddevice.ValidFoundational;
import io.mosip.masterdata.validator.registereddevice.ValidPurpose;
import lombok.Data;

/**
 * @author Ramadurai Pandian
 *
 */
@Data
@ValidFoundational(baseField = "certificationLevel", matchField = { "foundationalTrustProviderId" })
public class DeviceData {

	
	@StringFormatter(min = 1, max = 36)
	private String deviceId;

	@ValidPurpose(message = "Invalid Purpose received")
	private String purpose;

	private DeviceInfo deviceInfo;

	private String foundationalTrustProviderId;

}
