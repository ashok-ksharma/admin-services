package io.mosip.masterdata.repository;

import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.masterdata.entity.DeviceRegisterHistory;
import io.mosip.masterdata.entity.id.DeviceRegisterHistoryId;

@Repository
public interface DeviceRegisterHistoryRepository
		extends BaseRepository<DeviceRegisterHistory, DeviceRegisterHistoryId> {

}
