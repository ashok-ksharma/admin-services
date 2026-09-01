package io.mosip.admin.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;


/*
 * Renamed from "authorizedRoles" during the admin-services merge.
 * io.mosip.kernel.masterdata.config.AuthorizedRolesDto also declares
 * @Component("authorizedRoles") explicitly, so one of the two had to give up the name or
 * the application would fail to start with ConflictingBeanDefinitionException.
 *
 * Admin's was renamed rather than masterdata's because the name is referenced from SpEL in
 * @PreAuthorize expressions, which no compiler checks: 10 references across 5 files here,
 * against 286 across 53 files in masterdata.
 *
 * The two DTOs are not merged - they bind different prefixes (mosip.role.admin here,
 * mosip.role.admin.masterdata there) onto different role sets, and merging them could
 * silently change an authorization decision. Binding is by prefix, not by bean name, so
 * this rename does not affect it (merge plan section 4.2).
 */
@Component("adminAuthorizedRoles")
@ConfigurationProperties(prefix = "mosip.role.admin")
@Getter
@Setter
public class AuthorizedRolesDto {

	//Bulk data upload controller
	private List<String> postbulkupload;
	
	private List<String> getbulkuploadtranscationtranscationid;
	
	private List<String> getbulkuploadgetalltransactions;

	//Audit manager proxy controller
	private List<String> postauditmanagerlog;

	//packet status update controller
	private List<String> getpacketstatusupdate;


	//admin lostRid controller

	private List<String> getlostRiddetailsrid;
	private List<String> postlostRid;

	//applicant Details controller

	private List<String> getapplicantDetailsrid;
	private List<String> getapplicantDetailsgetLoginDetails;
	private List<String> getriddigitalcardrid;


	// keymanager controller
	private List<String> getgeneratecsrcertificateapplicationidreferenceid;
	private List<String> postuploadcertificate;
	private List<String> postgeneratecsr;
	private List<String> postuploadotherdomaincertificate;

	
}