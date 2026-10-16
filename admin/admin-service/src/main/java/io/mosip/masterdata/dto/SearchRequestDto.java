package io.mosip.masterdata.dto;

import java.util.List;

import io.mosip.masterdata.dto.request.Pagination;
import io.mosip.masterdata.dto.request.SearchSort;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequestDto {

	private List<SearchRequestFilter> filters;

	private SearchSort sort;

	private Pagination pagination;

}
