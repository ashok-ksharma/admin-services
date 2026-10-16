package io.mosip.masterdata.service;

import io.mosip.masterdata.dto.request.RequestDTO;
import io.mosip.masterdata.dto.response.ResponseDTO;

public interface ApplicantTypeService {

	/**
	 * This method return the applicant id.
	 * 
	 * @param dto Request dto
	 * @return applicant id
	 */
	public ResponseDTO getApplicantType(RequestDTO dto);

}
