package io.mosip.masterdata.utils;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.masterdata.constant.DeviceSpecificationErrorCode;
import io.mosip.masterdata.constant.DeviceTypeErrorCode;
import io.mosip.masterdata.constant.RegistrationCenterErrorCode;
import io.mosip.masterdata.entity.DeviceSpecification;
import io.mosip.masterdata.entity.DeviceType;
import io.mosip.masterdata.entity.RegistrationCenter;
import io.mosip.masterdata.exception.MasterDataServiceException;
import io.mosip.masterdata.repository.DeviceSpecificationRepository;
import io.mosip.masterdata.repository.DeviceTypeRepository;
import io.mosip.masterdata.repository.RegistrationCenterRepository;

@Component
public class DeviceUtils {

	@Autowired
	private DeviceTypeRepository deviceTypeRepository;

	@Autowired
	private DeviceSpecificationRepository deviceSpecificationRepository;

	

	@Autowired
	private RegistrationCenterRepository centerRepository;

	public List<DeviceSpecification> getDeviceSpec() {
		try {
			return deviceSpecificationRepository.findAllDeviceSpecByIsActiveAndIsDeletedIsNullOrFalse();
		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					DeviceSpecificationErrorCode.DEVICE_SPECIFICATION_DATA_FETCH_EXCEPTION.getErrorCode(),
					DeviceSpecificationErrorCode.DEVICE_SPECIFICATION_DATA_FETCH_EXCEPTION.getErrorMessage());
		}
	}

	public List<DeviceType> getDeviceTypes() {
		try {
			return deviceTypeRepository.findAllDeviceTypeByIsActiveAndIsDeletedFalseOrNull();

		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(DeviceTypeErrorCode.DEVICE_TYPE_FETCH_EXCEPTION.getErrorCode(),
					DeviceTypeErrorCode.DEVICE_TYPE_FETCH_EXCEPTION.getErrorMessage());
		}
	}

	

	public List<RegistrationCenter> getAllRegistrationCenters(String langCode) {
		try {
			return centerRepository.findAllByIsDeletedFalseOrIsDeletedIsNullAndLangCode(langCode);
		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorCode(),
					RegistrationCenterErrorCode.REGISTRATION_CENTER_FETCH_EXCEPTION.getErrorMessage());
		}
	}

}
