package io.mosip.masterdata.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import io.mosip.masterdata.dto.KeyValues;
import lombok.Data;

/**
 * 
 * @author Bal Vikash Sharma
 *
 */
@Data
public class RequestDTO {
	@NotNull
	private List<KeyValues<String, Object>> attributes;
}