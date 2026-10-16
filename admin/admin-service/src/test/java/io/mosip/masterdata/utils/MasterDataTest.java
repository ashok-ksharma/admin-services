package io.mosip.masterdata.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.mergedadmin.web.ApiPathPrefixConfig;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.masterdata.dto.SearchDtoWithoutLangCode;
import io.mosip.masterdata.dto.request.*;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MasterDataTest {

	public static void checkResponse(MvcResult rst, String expectedCode) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			if (rst.getResponse().getContentAsString().isEmpty() && rst.getResponse().getStatus() == 404) {
				assertEquals(404,rst.getResponse().getStatus());

			} else {
				Map m = mapper.readValue(rst.getResponse().getContentAsString(), Map.class);
				assertEquals(200,rst.getResponse().getStatus());
				assertEquals(expectedCode, getErrorCode(m));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	private static String getErrorCode(Map map){
		if(map.containsKey("errors") && null != map.get("errors")){
			List<Map<String ,String>> data=(List<Map<String, String>>) map.get("errors");
			return data.isEmpty()?null:data.get(0).get("errorCode");
		}
		return null;
	}

	public static void checkErrorResponse(MvcResult rst) {
		assertEquals(rst.getResponse().getStatus(), 500);
	}

	/**
	 * GET on masterdata's external path, as a client sends it. This test context maps
	 * masterdata's controllers without the /v1/masterdata prefix, so the prefix is passed as
	 * the context path: controllers resolve as usual and the full URI carries the prefix.
	 */
	public static MockHttpServletRequestBuilder getOnMasterdataPath(String path) {
		return MockMvcRequestBuilders.get(ApiPathPrefixConfig.MASTERDATA_PREFIX + path)
				.contextPath(ApiPathPrefixConfig.MASTERDATA_PREFIX);
	}

	/** No controller maps the request: masterdata's catch-all answers HTTP 500 with KER-MSD-500. */
	public static void checkUnmappedRequestResponse(MvcResult rst) throws Exception {
		checkErrorResponse(rst);
		Map m = new ObjectMapper().readValue(rst.getResponse().getContentAsString(), Map.class);
		assertEquals("KER-MSD-500", getErrorCode(m));
	}

	public static RequestWrapper<SearchDtoWithoutLangCode> commonSearchDtoWithoutLangCode(String sortField,
			String seachSort, String columnName, String value, String type) {
		RequestWrapper<SearchDtoWithoutLangCode> sr = new RequestWrapper<>();
		SearchDtoWithoutLangCode sc = new SearchDtoWithoutLangCode();
		List<io.mosip.masterdata.dto.request.SearchFilter> ls = new ArrayList<>();
		io.mosip.masterdata.dto.request.SearchFilter sf = new io.mosip.masterdata.dto.request.SearchFilter();
		List<SearchSort> ss = new ArrayList<>();
		Pagination pagination = new Pagination(0, 1);
		SearchSort s = new SearchSort(sortField, seachSort);
		ss.add(s);
		sf.setColumnName(columnName);
		sf.setType(type);
		sf.setValue(value);
		ls.add(sf);
		sc.setFilters(ls);
		sc.setLanguageCode("eng");
		sc.setPagination(pagination);
		sc.setSort(ss);

		sr.setRequest(sc);
		return sr;
	}

	public static RequestWrapper<SearchDto> commonSearchDto(String seachSortField, String seachSortFiled,
			String columnName, String value, String type) {
		RequestWrapper<SearchDto> sr = new RequestWrapper<>();
		SearchDto sc = new SearchDto();
		List<io.mosip.masterdata.dto.request.SearchFilter> ls = new ArrayList<>();
		io.mosip.masterdata.dto.request.SearchFilter sf = new io.mosip.masterdata.dto.request.SearchFilter();
		List<SearchSort> ss = new ArrayList<>();
		Pagination pagination = new Pagination(0, 1);
		SearchSort s = new SearchSort(seachSortField, seachSortFiled);
		ss.add(s);
		sf.setColumnName(columnName);
		sf.setType(type);
		sf.setValue(value);
		ls.add(sf);
		sc.setFilters(ls);
		sc.setLanguageCode("eng");
		sc.setPagination(pagination);
		sc.setSort(ss);

		sr.setRequest(sc);
		return sr;
	}

	public static RequestWrapper<FilterValueDto> commonFilterValueDto(String columnName, String txt, String type) {
		RequestWrapper<FilterValueDto> filValDto;
		FilterValueDto f = new FilterValueDto();
		FilterDto fdto = new FilterDto();
		fdto.setColumnName(columnName);
		fdto.setText(txt);
		fdto.setType(type);
		List<FilterDto> lf = new ArrayList<>();
		lf.add(fdto);
		f.setLanguageCode("eng");
		f.setOptionalFilters(null);
		f.setFilters(lf);
		filValDto = new RequestWrapper<>();
		filValDto.setRequest(f);
		return filValDto;
	}

}
