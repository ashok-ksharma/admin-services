package io.mosip.masterdata.service;

import java.util.List;

import io.mosip.masterdata.dto.BiometricAttributeDto;
import io.mosip.masterdata.entity.id.CodeAndLanguageCodeID;

/**
 * 
 * @author Uday
 * @since 1.0.0
 *
 */

public interface BiometricAttributeService {

	/**
	 * 
	 * Method to fetch list of biometricAttribute for given biometric type code and
	 * language code
	 * 
	 * @param biometricTypeCode input as biometricTypeCode
	 * @param langCode          input as langCode
	 * @return List of BiometricAttributeDto
	 */
	List<BiometricAttributeDto> getBiometricAttribute(String biometricTypeCode, String langCode);

	/**
	 * Function to save biometricAttribute Details to the Database
	 * 
	 * @param biometricAttribute biometric attribute dto
	 * 
	 * @return {@link CodeAndLanguageCodeID}
	 */
	CodeAndLanguageCodeID createBiometricAttribute(BiometricAttributeDto biometricAttribute);

}
