package io.mosip.masterdata.service;

import io.mosip.masterdata.dto.PacketWorkflowActionResponseDTO;
import io.mosip.masterdata.dto.PacketWorkflowResumeRequestDto;
import io.mosip.masterdata.dto.RegProcResponseWrapper;
import io.mosip.masterdata.dto.SearchDtoWithoutLangCode;
import io.mosip.masterdata.dto.SearchResponseDto;

public interface PacketWorkflowActionService {

	RegProcResponseWrapper<PacketWorkflowActionResponseDTO> resumePacket(PacketWorkflowResumeRequestDto request);

	RegProcResponseWrapper<SearchResponseDto> searchPacket(SearchDtoWithoutLangCode request);

}
