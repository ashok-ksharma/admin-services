package io.mosip.masterdata.dto;

import lombok.Data;

@Data
public class DeRegisterDeviceRequestDto {
	private DeviceDeRegDto device;
	private String signature;
}
