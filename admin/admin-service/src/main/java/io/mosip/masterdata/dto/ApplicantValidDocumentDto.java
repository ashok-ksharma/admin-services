package io.mosip.masterdata.dto;


import lombok.Data;
import java.util.Collection;
import io.mosip.masterdata.dto.getresponse.DocumentCategoryAndTypeResponseDto;

/**
 * 
 * @author Srinivasan
 *
 */
@Data
public class ApplicantValidDocumentDto {
	private String appTypeCode;
	private String langCode;
	private Boolean isActive;
	private Collection<DocumentCategoryAndTypeResponseDto> documentCategories;

}
