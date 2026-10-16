package io.mosip.masterdata.dto;

import lombok.Data;

@Data
public class DeviceRegisterDto {
	private DeviceDataDto deviceData;
	private String dpSignature;
}
