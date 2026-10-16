package io.mosip.masterdata.service;

import java.time.LocalDateTime;
import java.util.List;

import io.mosip.masterdata.dto.DynamicFieldConsolidateResponseDto;
import io.mosip.masterdata.dto.DynamicFieldDefDto;
import io.mosip.masterdata.dto.DynamicFieldDto;
import io.mosip.masterdata.dto.DynamicFieldPutDto;
import io.mosip.masterdata.dto.getresponse.DynamicFieldResponseDto;
import io.mosip.masterdata.dto.getresponse.DynamicFieldSearchResponseDto;
import io.mosip.masterdata.dto.getresponse.PageDto;
import io.mosip.masterdata.dto.getresponse.StatusResponseDto;
import io.mosip.masterdata.dto.getresponse.extn.DynamicFieldExtnDto;
import io.mosip.masterdata.dto.request.FilterValueDto;
import io.mosip.masterdata.dto.request.SearchDto;
import io.mosip.masterdata.dto.response.FilterResponseCodeDto;
import io.mosip.masterdata.dto.response.PageResponseDto;

/**
 * Methods to create / update / inactivate / addValues dynamic field
 * 
 * @author anusha
 *
 */
public interface DynamicFieldService {
	
	/**
	 * Fetch all dynamic fields
	 * @param pageNumber
	 * @param pageSize
	 * @param sortBy
	 * @param orderBy
	 * @param langCode
	 * @return
	 */
	public PageDto<DynamicFieldExtnDto> getAllDynamicField(int pageNumber, int pageSize, String sortBy, String orderBy, String langCode,
														   LocalDateTime lastUpdated, LocalDateTime currentTimestamp);
	
	/**
	 * create dynamic field
	 * @param dto
	 * @return
	 */
	public DynamicFieldResponseDto createDynamicField(DynamicFieldDto dto);
	
	/**
	 * update dynamic field
	 * @param dto
	 * @return
	 */
	public DynamicFieldResponseDto updateDynamicField(String id, DynamicFieldPutDto dto);

	/**
	 * Updates status of all entries with same field name
	 * @param fieldName
	 * @param isActive
	 * @return
	 */
	public StatusResponseDto updateDynamicFieldStatus(String fieldName, boolean isActive);

	/**
	 * Updates status of row of provided id
	 * @param id
	 * @param isActive
	 * @return
	 */
	public StatusResponseDto updateDynamicFieldValueStatus(String id, boolean isActive);

	public StatusResponseDto deleteDynamicFieldValue(String id);

	public StatusResponseDto deleteDynamicField(String fieldName);

	public PageResponseDto<DynamicFieldSearchResponseDto> searchDynamicFields(SearchDto request);

	public List<String> getDistinctDynamicFields();

	public List<DynamicFieldDefDto> getDistinctDynamicFields(String langCode);

    FilterResponseCodeDto dynamicfieldFilterValues(FilterValueDto request);

 	public DynamicFieldConsolidateResponseDto getDynamicFieldByNameAndLangcode(String fieldName,String langCode,boolean withValue);


	List<DynamicFieldExtnDto> getAllDynamicFieldByName(String fieldName);
}
