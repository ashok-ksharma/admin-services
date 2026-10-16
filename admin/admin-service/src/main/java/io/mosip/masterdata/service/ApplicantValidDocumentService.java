package io.mosip.masterdata.service;

import java.util.List;

import io.mosip.masterdata.dto.ApplicantValidDocumentDto;

public interface ApplicantValidDocumentService {

	public ApplicantValidDocumentDto getDocumentCategoryAndTypes(String applicantTypeCode, List<String> langCodes);

}
