package io.mosip.masterdata.dto.getresponse;

import java.util.List;

import io.mosip.masterdata.dto.DocumentTypeDto;
import lombok.Data;

/**
 * 
 * @author Uday Kumar
 * @since 1.0.0
 */

@Data
public class DocumentTypeResponseDto {
	private List<DocumentTypeDto> documenttypes;

}
