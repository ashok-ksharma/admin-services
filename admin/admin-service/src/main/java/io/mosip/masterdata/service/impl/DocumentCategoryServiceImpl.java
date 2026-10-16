package io.mosip.masterdata.service.impl;

import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.util.EmptyCheckUtils;
import io.mosip.masterdata.constant.DeviceTypeErrorCode;
import io.mosip.masterdata.constant.DocumentCategoryErrorCode;
import io.mosip.masterdata.constant.LanguageErrorCode;
import io.mosip.masterdata.constant.MasterDataConstant;
import io.mosip.masterdata.dto.DocumentCategoryDto;
import io.mosip.masterdata.dto.DocumentCategoryPutDto;
import io.mosip.masterdata.dto.getresponse.DocumentCategoryResponseDto;
import io.mosip.masterdata.dto.getresponse.PageDto;
import io.mosip.masterdata.dto.getresponse.StatusResponseDto;
import io.mosip.masterdata.dto.getresponse.extn.DocumentCategoryExtnDto;
import io.mosip.masterdata.dto.postresponse.CodeResponseDto;
import io.mosip.masterdata.dto.request.FilterDto;
import io.mosip.masterdata.dto.request.FilterValueDto;
import io.mosip.masterdata.dto.request.Pagination;
import io.mosip.masterdata.dto.request.SearchDto;
import io.mosip.masterdata.dto.request.SearchSort;
import io.mosip.masterdata.dto.response.ColumnValue;
import io.mosip.masterdata.dto.response.FilterResponseDto;
import io.mosip.masterdata.dto.response.FilterResult;
import io.mosip.masterdata.dto.response.PageResponseDto;
import io.mosip.masterdata.entity.DocumentCategory;
import io.mosip.masterdata.entity.ValidDocument;
import io.mosip.masterdata.entity.id.CodeAndLanguageCodeID;
import io.mosip.masterdata.exception.DataNotFoundException;
import io.mosip.masterdata.exception.MasterDataServiceException;
import io.mosip.masterdata.exception.RequestException;
import io.mosip.masterdata.repository.DocumentCategoryRepository;
import io.mosip.masterdata.repository.ValidDocumentRepository;
import io.mosip.masterdata.service.DocumentCategoryService;
import io.mosip.masterdata.utils.AuditUtil;
import io.mosip.masterdata.utils.ExceptionUtils;
import io.mosip.masterdata.utils.MapperUtils;
import io.mosip.masterdata.utils.MasterDataFilterHelper;
import io.mosip.masterdata.utils.MasterdataCreationUtil;
import io.mosip.masterdata.utils.MasterdataSearchHelper;
import io.mosip.masterdata.utils.MetaDataUtils;
import io.mosip.masterdata.utils.PageUtils;
import io.mosip.masterdata.validator.FilterColumnEnum;
import io.mosip.masterdata.validator.FilterColumnValidator;
import io.mosip.masterdata.validator.FilterTypeValidator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Collections;

/**
 * This class have methods to fetch list of valid document category, create
 * document category based on provided data,update document category based on
 * data provided and delete document category based on id provided.
 * 
 * @author Neha
 * @author Ritesh Sinha
 * @author Uday Kumar
 * @since 1.0.0
 *
 */
@Service
@Transactional
public class DocumentCategoryServiceImpl implements DocumentCategoryService {

	@Autowired
	AuditUtil auditUtil;

	@Autowired
	private DocumentCategoryRepository documentCategoryRepository;

	@Autowired
	private ValidDocumentRepository validDocumentRepository;

	@Autowired
	private FilterTypeValidator filterTypeValidator;

	@Autowired
	private MasterdataSearchHelper masterDataSearchHelper;

	@Autowired
	private FilterColumnValidator filterColumnValidator;

	@Autowired
	private MasterDataFilterHelper masterDataFilterHelper;

	@Autowired
	private PageUtils pageUtils;

	private List<DocumentCategory> documentCategoryList = new ArrayList<>();

	private DocumentCategoryResponseDto documentCategoryResponseDto = new DocumentCategoryResponseDto();

	@Autowired
	private MasterdataCreationUtil masterdataCreationUtil;

	@Value("${mosip.supported-languages}")
	private String supportedLanguages;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * getAllDocumentCategory()
	 */

	@Cacheable(value = "document-category", key = "'documentcategory'")
	@Override
	public DocumentCategoryResponseDto getAllDocumentCategory() {
		List<DocumentCategoryDto> documentCategoryDtoList = new ArrayList<>();
		try {
			documentCategoryList = documentCategoryRepository
					.findAllByIsDeletedFalseOrIsDeletedIsNull(DocumentCategory.class);
		} catch (DataAccessException | DataAccessLayerException e) {
			throw new MasterDataServiceException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_FETCH_EXCEPTION.getErrorCode(),
					e.getMessage() + ExceptionUtils.parseException(e));
		}

		if (!(documentCategoryList.isEmpty())) {
			documentCategoryList.forEach(documentCategory -> {
				DocumentCategoryDto documentCategoryDto = new DocumentCategoryDto();
				MapperUtils.map(documentCategory, documentCategoryDto);
				documentCategoryDtoList.add(documentCategoryDto);
			});
		} else {
			throw new DataNotFoundException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage());
		}
		documentCategoryResponseDto.setDocumentcategories(documentCategoryDtoList);
		return documentCategoryResponseDto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * getAllDocumentCategoryByLaguageCode(java.lang.String)
	 */
	@Cacheable(value = "document-category", key = "'documentcategory'.concat('-').concat(#langCode)", condition="#langCode != null")
	@Override
	public DocumentCategoryResponseDto getAllDocumentCategoryByLaguageCode(String langCode) {
		validateLangCode(langCode.toLowerCase());
		List<DocumentCategoryDto> documentCategoryDtoList = new ArrayList<>();
		try {
			documentCategoryList = documentCategoryRepository
					.findAllByLangCodeAndIsDeletedFalseOrIsDeletedIsNull(langCode.toLowerCase());
		} catch (DataAccessException | DataAccessLayerException e) {
			throw new MasterDataServiceException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_FETCH_EXCEPTION.getErrorCode(),
					e.getMessage() + ExceptionUtils.parseException(e));
		}

		if (!(documentCategoryList.isEmpty())) {
			documentCategoryList.forEach(documentCategory -> {
				DocumentCategoryDto documentCategoryDto = new DocumentCategoryDto();
				MapperUtils.map(documentCategory, documentCategoryDto);
				documentCategoryDtoList.add(documentCategoryDto);
			});
		} else {
			throw new DataNotFoundException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage());
		}
		documentCategoryResponseDto.setDocumentcategories(documentCategoryDtoList);
		return documentCategoryResponseDto;
	}

	private void validateLangCode(String langCode) {
		if (langCode == null || langCode.trim().isEmpty()) {
			throw new DataNotFoundException(LanguageErrorCode.NO_LANGUAGE_FOUND_EXCEPTION.getErrorCode(),
					LanguageErrorCode.NO_LANGUAGE_FOUND_EXCEPTION.getErrorMessage());
		}
		Set<String> supportedLanguagesSet = new HashSet<>(Arrays.asList(supportedLanguages.split(",")));
		if (!supportedLanguagesSet.contains(langCode)){
			throw new DataNotFoundException(LanguageErrorCode.INVALID_LANGUAGE_CODE.getErrorCode(),
					LanguageErrorCode.INVALID_LANGUAGE_CODE.getErrorMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * getDocumentCategoryByCodeAndLangCode(java.lang.String, java.lang.String)
	 */
	@Override
	public DocumentCategoryResponseDto getDocumentCategoryByCodeAndLangCode(String code, String langCode) {
		List<DocumentCategoryDto> documentCategoryDtoList = new ArrayList<>();
		DocumentCategory documentCategory;
		DocumentCategoryDto documentCategoryDto;
		try {
			documentCategory = documentCategoryRepository.findByCodeAndLangCodeAndIsDeletedFalseOrIsDeletedIsNull(code,
					langCode);
		} catch (DataAccessException | DataAccessLayerException e) {
			throw new MasterDataServiceException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_FETCH_EXCEPTION.getErrorCode(),
					e.getMessage() + ExceptionUtils.parseException(e));
		}

		if (documentCategory != null) {
			documentCategoryDto = MapperUtils.map(documentCategory, DocumentCategoryDto.class);
		} else {
			throw new DataNotFoundException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage());
		}
		documentCategoryDtoList.add(documentCategoryDto);
		documentCategoryResponseDto.setDocumentcategories(documentCategoryDtoList);
		return documentCategoryResponseDto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * createDocumentCategory(io.mosip.masterdata.dto.RequestDto)
	 */
	@CacheEvict(value = "document-category", allEntries = true)
	@Override
	public CodeAndLanguageCodeID createDocumentCategory(DocumentCategoryDto category) {
		DocumentCategory documentCategory;
		try {

			Optional<DocumentCategory> existingCategory = documentCategoryRepository.findFirstByCodeAndIsDeletedFalseOrIsDeletedIsNull(category.getCode());
			category = masterdataCreationUtil.createMasterData(DocumentCategory.class, category);
			DocumentCategory entity = MetaDataUtils.setCreateMetaData(category, DocumentCategory.class);
			if(existingCategory.isPresent()) { entity.setIsActive(existingCategory.get().getIsActive()); }
			documentCategory = documentCategoryRepository.create(entity);
		} catch (DataAccessLayerException | DataAccessException | IllegalArgumentException | IllegalAccessException
				| NoSuchFieldException | SecurityException e) {
			auditUtil.auditRequest(
					String.format(MasterDataConstant.FAILURE_CREATE, DocumentCategory.class.getCanonicalName()),
					MasterDataConstant.AUDIT_SYSTEM,
					String.format(MasterDataConstant.FAILURE_DESC,
							DocumentCategoryErrorCode.DOCUMENT_CATEGORY_INSERT_EXCEPTION.getErrorCode(),
							DocumentCategoryErrorCode.DOCUMENT_CATEGORY_INSERT_EXCEPTION.getErrorMessage()),
					"ADM-802");
			throw new MasterDataServiceException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_INSERT_EXCEPTION.getErrorCode(),
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_INSERT_EXCEPTION.getErrorMessage() + " "
							+ ExceptionUtils.parseException(e));
		}
		CodeAndLanguageCodeID codeLangCodeId = new CodeAndLanguageCodeID();
		MapperUtils.map(documentCategory, codeLangCodeId);
		auditUtil.auditRequest(
				String.format(MasterDataConstant.SUCCESSFUL_CREATE, DocumentCategory.class.getSimpleName()),
				MasterDataConstant.AUDIT_SYSTEM, String.format(MasterDataConstant.SUCCESSFUL_CREATE_DESC,
						DocumentCategory.class.getSimpleName(), codeLangCodeId.getCode()),"ADM-939");
		return codeLangCodeId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * updateDocumentCategory(io.mosip.masterdata.dto.RequestDto)
	 */
	@CacheEvict(value = "document-category", allEntries = true)
	@Override
	public CodeAndLanguageCodeID updateDocumentCategory(DocumentCategoryPutDto categoryDto) {

		CodeAndLanguageCodeID documentCategoryId = new CodeAndLanguageCodeID();

		MapperUtils.mapFieldValues(categoryDto, documentCategoryId);
		try {

			DocumentCategory documentCategory = documentCategoryRepository.findByCodeAndLangCode(categoryDto.getCode(),
					categoryDto.getLangCode());

			if (documentCategory != null) {
				/*
				 * if(!categoryDto.getIsActive()) { List<ValidDocument> validDocuments =
				 * validDocumentRepository .findByDocCategoryCode(categoryDto.getCode()); if
				 * (!EmptyCheckUtils.isNullEmpty(validDocuments)) { throw new RequestException(
				 * DeviceTypeErrorCode.DEVICE_TYPE_UPDATE_MAPPING_EXCEPTION.getErrorCode(),
				 * DeviceTypeErrorCode.DEVICE_TYPE_UPDATE_MAPPING_EXCEPTION.getErrorMessage());
				 * } masterdataCreationUtil.updateMasterDataDeactivate(DocumentCategory.class,
				 * categoryDto.getCode()); }
				 */
				categoryDto = masterdataCreationUtil.updateMasterData(DocumentCategory.class, categoryDto);
				MetaDataUtils.setUpdateMetaData(categoryDto, documentCategory, false);
				documentCategoryRepository.update(documentCategory);
			} else {
				auditUtil.auditRequest(
						String.format(MasterDataConstant.FAILURE_UPDATE, DocumentCategory.class.getCanonicalName()),
						MasterDataConstant.AUDIT_SYSTEM,
						String.format(MasterDataConstant.FAILURE_DESC,
								DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
								DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage()),
						"ADM-803");
				throw new RequestException(
						DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
						DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage());
			}

		} catch (DataAccessLayerException | DataAccessException | IllegalArgumentException | IllegalAccessException
				| NoSuchFieldException | SecurityException e) {
			auditUtil.auditRequest(
					String.format(MasterDataConstant.FAILURE_UPDATE, DocumentCategory.class.getCanonicalName()),
					MasterDataConstant.AUDIT_SYSTEM,
					String.format(MasterDataConstant.FAILURE_DESC,
							DocumentCategoryErrorCode.DOCUMENT_CATEGORY_UPDATE_EXCEPTION.getErrorCode(),
							DocumentCategoryErrorCode.DOCUMENT_CATEGORY_UPDATE_EXCEPTION.getErrorMessage()),
					"ADM-804");
			throw new MasterDataServiceException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_UPDATE_EXCEPTION.getErrorCode(),
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_UPDATE_EXCEPTION.getErrorMessage() + " "
							+ ExceptionUtils.parseException(e));
		}

		auditUtil.auditRequest(
				String.format(MasterDataConstant.SUCCESSFUL_UPDATE, DocumentCategory.class.getSimpleName()),
				MasterDataConstant.AUDIT_SYSTEM, String.format(MasterDataConstant.SUCCESSFUL_UPDATE_DESC,
						DocumentCategory.class.getSimpleName(), documentCategoryId.getCode()),"ADM-940");
		return documentCategoryId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * deleteDocumentCategory(java.lang.String)
	 */
	@CacheEvict(value = "document-category", allEntries = true)
	@Override
	public CodeResponseDto deleteDocumentCategory(String code) {

		try {
			List<ValidDocument> validDocument = validDocumentRepository.findByDocCategoryCode(code);

			if (!validDocument.isEmpty()) {
				throw new MasterDataServiceException(
						DocumentCategoryErrorCode.DOCUMENT_CATEGORY_DELETE_DEPENDENCY_EXCEPTION.getErrorCode(),
						DocumentCategoryErrorCode.DOCUMENT_CATEGORY_DELETE_DEPENDENCY_EXCEPTION.getErrorMessage());
			}

			int updatedRows = documentCategoryRepository.deleteDocumentCategory(LocalDateTime.now(ZoneId.of("UTC")),
					code, MetaDataUtils.getContextUser());
			if (updatedRows < 1) {

				throw new RequestException(
						DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
						DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage());
			}

		} catch (DataAccessLayerException | DataAccessException e) {
			throw new MasterDataServiceException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_DELETE_EXCEPTION.getErrorCode(),
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_DELETE_EXCEPTION.getErrorMessage() + " "
							+ ExceptionUtils.parseException(e));
		}
		CodeResponseDto responseDto = new CodeResponseDto();
		responseDto.setCode(code);
		return responseDto;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * getAllDocCategories(int, int, java.lang.String, java.lang.String)
	 */
	@Override
	public PageDto<DocumentCategoryExtnDto> getAllDocCategories(int pageNumber, int pageSize, String sortBy,
			String orderBy) {
		List<DocumentCategoryExtnDto> docCategories = null;
		PageDto<DocumentCategoryExtnDto> docCategoriesPages = null;
		try {
			Page<DocumentCategory> pageData = documentCategoryRepository
					.findAll(PageRequest.of(pageNumber, pageSize, Sort.by(Direction.fromString(orderBy), sortBy)));
			if (pageData != null && pageData.getContent() != null && !pageData.getContent().isEmpty()) {
				docCategories = MapperUtils.mapAll(pageData.getContent(), DocumentCategoryExtnDto.class);
				docCategoriesPages = new PageDto<>(pageData.getNumber(), pageData.getTotalPages(),
						pageData.getTotalElements(), docCategories);
			} else {
				throw new DataNotFoundException(
						DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
						DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage());
			}
		} catch (DataAccessException | DataAccessLayerException e) {
			throw new MasterDataServiceException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_FETCH_EXCEPTION.getErrorCode(),
					e.getMessage() + ExceptionUtils.parseException(e));
		}
		return docCategoriesPages;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * searchDocCategories(io.mosip.masterdata.dto.request.SearchDto)
	 */
	@Override
	public PageResponseDto<DocumentCategoryExtnDto> searchDocCategories(SearchDto dto) {
		PageResponseDto<DocumentCategoryExtnDto> pageDto = new PageResponseDto<>();
		List<DocumentCategoryExtnDto> documentCategories = null;

		pageUtils.validateSortField(DocumentCategory.class, dto.getSort());
		if (filterTypeValidator.validate(DocumentCategoryExtnDto.class, dto.getFilters())) {
			Pagination pagination = dto.getPagination();
			List<SearchSort> sort = dto.getSort();
			pageUtils.validateSortField(DocumentCategoryExtnDto.class, DocumentCategory.class, sort);
			dto.setPagination(new Pagination(0, Integer.MAX_VALUE));
			dto.setSort(Collections.emptyList());
			Page<DocumentCategory> page = masterDataSearchHelper.searchMasterdata(DocumentCategory.class, dto, null);

			if (page.getContent() != null && !page.getContent().isEmpty()) {
				documentCategories = MapperUtils.mapAll(page.getContent(), DocumentCategoryExtnDto.class);
				pageDto = pageUtils.sortPage(documentCategories, sort, pagination);
			}
		}
		return pageDto;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.mosip.masterdata.service.DocumentCategoryService#
	 * docCategoriesFilterValues(io.mosip.masterdata.dto.request.
	 * FilterValueDto)
	 */
	@Override
	public FilterResponseDto docCategoriesFilterValues(FilterValueDto filterValueDto) {
		FilterResponseDto filterResponseDto = new FilterResponseDto();
		List<ColumnValue> columnValueList = new ArrayList<>();
		List<FilterDto> filLst = new ArrayList<>();
		filterValueDto.getFilters().forEach(fil -> {
			if (fil.getType().isEmpty() || fil.getType().isBlank()) {
				FilterDto f = fil;
				f.setColumnName(fil.getColumnName());
				f.setText(fil.getText());
				f.setType(FilterColumnEnum.ALL.toString());
				filLst.add(f);
			} else {
				filLst.add(fil);
			}
		});
		filterValueDto.setFilters(filLst);

		if (filterColumnValidator.validate(FilterDto.class, filterValueDto.getFilters(), DocumentCategory.class)) {
			for (FilterDto filterDto : filterValueDto.getFilters()) {
				FilterResult<String> filterResult = masterDataFilterHelper.filterValues(DocumentCategory.class, filterDto,
								filterValueDto);

				filterResult.getFilterData().forEach(filterValue -> {
							if (filterValue != null) {
								ColumnValue columnValue = new ColumnValue();
								columnValue.setFieldID(filterDto.getColumnName());
								columnValue.setFieldValue(filterValue);
								columnValueList.add(columnValue);
							}
						});
				filterResponseDto.setTotalCount(filterResult.getTotalCount());
			}
			filterResponseDto.setFilters(columnValueList);
		}
		return filterResponseDto;
	}

	@CacheEvict(value = "document-category", allEntries = true)
	@Override
	public StatusResponseDto updateDocumentCategory(String code, boolean isActive) {
		// TODO Auto-generated method stub
		StatusResponseDto response = new StatusResponseDto();

		List<DocumentCategory> documentCategories = null;
		try {
			documentCategories = documentCategoryRepository.findtoUpdateDocumentCategoryByCode(code);
		} catch (DataAccessLayerException | DataAccessException | IllegalArgumentException | SecurityException e) {
			auditUtil.auditRequest(
					String.format(MasterDataConstant.FAILURE_UPDATE, DocumentCategory.class.getCanonicalName()),
					MasterDataConstant.AUDIT_SYSTEM,
					String.format(MasterDataConstant.FAILURE_DESC,
							DocumentCategoryErrorCode.DOCUMENT_CATEGORY_UPDATE_EXCEPTION.getErrorCode(),
							DocumentCategoryErrorCode.DOCUMENT_CATEGORY_UPDATE_EXCEPTION.getErrorMessage()),
					"ADM-805");
			throw new MasterDataServiceException(
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_UPDATE_EXCEPTION.getErrorCode(),
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_UPDATE_EXCEPTION.getErrorMessage() + " "
							+ ExceptionUtils.parseException(e));
		}
		if (documentCategories != null && !documentCategories.isEmpty()) {
			if (!isActive) {
				List<ValidDocument> validDocuments = validDocumentRepository.findByDocCategoryCode(code);
				if (!EmptyCheckUtils.isNullEmpty(validDocuments)) {
					throw new RequestException(DeviceTypeErrorCode.DEVICE_TYPE_UPDATE_MAPPING_EXCEPTION.getErrorCode(),
							DeviceTypeErrorCode.DEVICE_TYPE_UPDATE_MAPPING_EXCEPTION.getErrorMessage());
				}
			}
			masterdataCreationUtil.updateMasterDataStatus(DocumentCategory.class, code, isActive, "code");
		} else {
			auditUtil.auditRequest(
					String.format(MasterDataConstant.FAILURE_UPDATE, DocumentCategory.class.getCanonicalName()),
					MasterDataConstant.AUDIT_SYSTEM,
					String.format(MasterDataConstant.FAILURE_DESC,
							DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
							DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage()),
					"ADM-806");
			throw new RequestException(DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorCode(),
					DocumentCategoryErrorCode.DOCUMENT_CATEGORY_NOT_FOUND_EXCEPTION.getErrorMessage());
		}
		response.setStatus("Status updated successfully for Document Categories");
		return response;
	}

}
