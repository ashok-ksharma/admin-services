package io.mosip.masterdata.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import io.mosip.masterdata.dto.DeRegisterDevicePostDto;
import io.mosip.masterdata.dto.DeviceDeRegisterResponse;
import io.mosip.masterdata.dto.EncodedRegisteredDeviceResponse;
import io.mosip.masterdata.dto.RegisteredDevicePostReqDto;
import io.mosip.masterdata.dto.getresponse.ResponseDto;
import io.mosip.masterdata.dto.getresponse.extn.RegisteredDeviceExtnDto;
import io.mosip.masterdata.dto.registerdevice.RegisteredDevicePostDto;

/**
 * 
 * @author Megha Tanga
 *
 */
public interface RegisteredDeviceService {

	/**
	 * Method to create Registered Device Provider
	 * 
	 * @param dto Regisetered Device Provider Dto from user
	 * @return RegisteredDeviceExtnDto Registered device Dto which has created
	 */
	// public RegisteredDeviceExtnDto
	// createRegisteredDevice(RegisteredDevicePostReqDto dto);

	/**
	 * Method to De-Register a device
	 * 
	 * @param deRegisterDevicePostDto
	 * @return DeviceDeRegisterResponse
	 */
	public String deRegisterDevice( DeRegisterDevicePostDto deRegisterDevicePostDto);

	/**
	 * Method to update status of a device
	 * 
	 * @param deviceCode
	 * @param statusCode
	 * @return ResponseDto
	 */
	public ResponseDto updateStatus(@NotBlank String deviceCode, @NotBlank String statusCode);

	/**
	 * Method to create a signed Register device
	 * 
	 * @param registeredDevicePostDto
	 * @return String
	 */
	public String signedRegisteredDevice(RegisteredDevicePostDto registeredDevicePostDto) throws Exception;

}
