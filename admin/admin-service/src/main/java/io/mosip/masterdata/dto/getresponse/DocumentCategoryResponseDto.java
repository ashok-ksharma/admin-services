package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.DocumentCategoryDto;
import lombok.Data;

/**
 * 
 * @author Neha
 * @since 1.0.0
 */
@Data
public class DocumentCategoryResponseDto {
	private List<DocumentCategoryDto> documentcategories;
}
