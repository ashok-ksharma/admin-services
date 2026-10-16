package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.DocumentTypeDto;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 
 * @author Uday Kumar
 * @since 1.0.0
 *
 */

@Data
@AllArgsConstructor
public class ValidDocumentTypeResponseDto {
	private List<DocumentTypeDto> documents;

}
