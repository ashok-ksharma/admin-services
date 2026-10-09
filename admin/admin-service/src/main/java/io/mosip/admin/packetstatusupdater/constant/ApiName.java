package io.mosip.admin.packetstatusupdater.constant;

/**
 * 
 * @author Sowmya
 *
 */
public enum ApiName {

	LOST_RID_API,CRYPTOMANAGERDECRYPT_API,RETRIEVE_IDENTITY_API,DIGITAL_CARD_STATUS_URL,

	PACKET_MANAGER_BIOMETRIC,PACKET_MANAGER_SEARCHFIELDS,

	/*
	 * Folded in from io.mosip.kernel.masterdata.constant.ApiName during the
	 * admin-services merge, which was deleted along with masterdata's duplicate
	 * RestClient. Every RestClient method takes ApiName in its signature, so one shared
	 * client needs one shared enum. The two enums had no constant in common, and each
	 * constant resolves through environment.getProperty(apiName.name()), so both
	 * services' lookups still hit exactly the property keys they hit before.
	 */
	PACKET_PAUSE_API,PACKET_RESUME_API;

}
