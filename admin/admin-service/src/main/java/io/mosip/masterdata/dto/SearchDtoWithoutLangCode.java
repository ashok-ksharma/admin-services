package io.mosip.masterdata.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.mosip.masterdata.dto.request.Pagination;
import io.mosip.masterdata.dto.request.SearchFilter;
import io.mosip.masterdata.dto.request.SearchSort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchDtoWithoutLangCode {

	@NotNull
	@Valid
	private List<SearchFilter> filters;

	@NotNull
	private List<SearchSort> sort;

	// @NotNull
	private Pagination pagination;

	private String languageCode;

}
