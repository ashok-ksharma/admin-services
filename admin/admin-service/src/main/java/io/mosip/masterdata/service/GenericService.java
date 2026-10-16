package io.mosip.masterdata.service;

import io.mosip.masterdata.dto.MissingDataDto;

import java.util.List;

public interface GenericService {

    List<MissingDataDto> getMissingData(Class entity, String langCode, String idFieldName, String fieldName);

    List<MissingDataDto> getMissingDynamicData(String langCode, String name);
}
