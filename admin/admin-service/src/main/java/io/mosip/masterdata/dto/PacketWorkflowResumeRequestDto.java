package io.mosip.masterdata.dto;

import lombok.Data;

@Data
public class PacketWorkflowResumeRequestDto {

	private String workflowId;

	private String workflowAction;

}
