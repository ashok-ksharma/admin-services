package io.mosip.admin.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import io.mosip.kernel.masterdata.entity.ApplicantValidDocument;
import io.mosip.kernel.masterdata.entity.Application;
import io.mosip.kernel.masterdata.entity.BiometricAttribute;
import io.mosip.kernel.masterdata.entity.BiometricType;
import io.mosip.kernel.masterdata.entity.BlocklistedWords;
import io.mosip.kernel.masterdata.entity.DaysOfWeek;
import io.mosip.kernel.masterdata.entity.Device;
import io.mosip.kernel.masterdata.entity.DeviceHistory;

import io.mosip.kernel.masterdata.entity.DeviceSpecification;
import io.mosip.kernel.masterdata.entity.DeviceType;
import io.mosip.kernel.masterdata.entity.DocumentCategory;
import io.mosip.kernel.masterdata.entity.DocumentType;
import io.mosip.kernel.masterdata.entity.DynamicField;
import io.mosip.kernel.masterdata.entity.ExceptionalHoliday;

import io.mosip.kernel.masterdata.entity.Gender;
import io.mosip.kernel.masterdata.entity.Holiday;
import io.mosip.kernel.masterdata.entity.IdType;
import io.mosip.kernel.masterdata.entity.IdentitySchema;
import io.mosip.kernel.masterdata.entity.IndividualType;
import io.mosip.kernel.masterdata.entity.Language;
import io.mosip.kernel.masterdata.entity.Location;
import io.mosip.kernel.masterdata.entity.LocationHierarchy;
import io.mosip.kernel.masterdata.entity.MOSIPDeviceService;
import io.mosip.kernel.masterdata.entity.MOSIPDeviceServiceHistory;
import io.mosip.kernel.masterdata.entity.Machine;
import io.mosip.kernel.masterdata.entity.MachineHistory;
import io.mosip.kernel.masterdata.entity.MachineSpecification;
import io.mosip.kernel.masterdata.entity.MachineType;
import io.mosip.kernel.masterdata.entity.ModuleDetail;
import io.mosip.kernel.masterdata.entity.ReasonCategory;
import io.mosip.kernel.masterdata.entity.ReasonList;
import io.mosip.kernel.masterdata.entity.RegExceptionalHoliday;
import io.mosip.kernel.masterdata.entity.RegWorkingNonWorking;
import io.mosip.kernel.masterdata.entity.RegisteredDevice;
import io.mosip.kernel.masterdata.entity.RegisteredDeviceHistory;
import io.mosip.kernel.masterdata.entity.RegistrationCenter;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterDevice;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterDeviceHistory;
import io.mosip.kernel.masterdata.entity.RegistrationCenterHistory;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterMachine;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterMachineDevice;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterMachineDeviceHistory;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterMachineHistory;
import io.mosip.kernel.masterdata.entity.RegistrationCenterType;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterUser;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterUserHistory;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterUserMachine;
import io.mosip.admin.bulkdataupload.entity.RegistrationCenterUserMachineHistory;
import io.mosip.kernel.masterdata.entity.RegistrationDeviceSubType;
import io.mosip.kernel.masterdata.entity.RegistrationDeviceType;
import io.mosip.kernel.masterdata.entity.SchemaDefinition;
import io.mosip.kernel.masterdata.entity.Template;
import io.mosip.kernel.masterdata.entity.TemplateFileFormat;
import io.mosip.kernel.masterdata.entity.TemplateType;
import io.mosip.kernel.masterdata.entity.Title;
import io.mosip.kernel.masterdata.entity.UserDetails;
import io.mosip.kernel.masterdata.entity.UserDetailsHistory;
import io.mosip.kernel.masterdata.entity.ValidDocument;
import io.mosip.kernel.masterdata.entity.Zone;
import io.mosip.kernel.masterdata.entity.ZoneUser;
import io.mosip.kernel.masterdata.entity.ZoneUserHistory;
/**
 * This class give the repository based on entity
 * @author dhanendra
 *
 */
@Component
public class Mapper {


	private static Map<Class,String> repositoryMap=new HashMap<Class, String>();
	
    private static Map<String,Class> entityMap=new HashMap<String, Class>();

	static {
		loadEntity();
		loadRepo();
	}

    private static void loadRepo() {
    	repositoryMap.put(Gender.class, "genderTypeRepository");
    	repositoryMap.put( ApplicantValidDocument.class, "applicantValidDocumentRepository");
		repositoryMap.put(Application.class, "applicationRepository");
		repositoryMap.put(BiometricAttribute.class, "biometricAttributeRepository");
		repositoryMap.put(BiometricType.class,"biometricTypeRepository");
		repositoryMap.put(BlocklistedWords.class,"blocklistedWordsRepository");
		repositoryMap.put(Device.class, "deviceRepository");
		repositoryMap.put(DeviceHistory.class,"deviceHistoryRepository");
		repositoryMap.put(DeviceSpecification.class, "deviceSpecificationRepository");
		repositoryMap.put(DeviceType.class,"deviceTypeRepository");
		repositoryMap.put(DocumentCategory.class,"documentCategoryRepository");
		repositoryMap.put(DocumentType.class,"documentTypeRepository");
		repositoryMap.put(DynamicField.class,"dynamicFieldRepository");
		repositoryMap.put(ExceptionalHoliday.class,"exceptionalHolidayRepository");
		repositoryMap.put(Holiday.class,"holidayRepository");
		repositoryMap.put(IdentitySchema.class, "identitySchemaRepository");
		repositoryMap.put(IdType.class,"idTypeRepository");
		repositoryMap.put(IndividualType.class,"individualTypeRepository");
		repositoryMap.put(Language.class, "languageRepository");
		repositoryMap.put(Location.class,"locationRepository");
		repositoryMap.put(LocationHierarchy.class,"locationHierarchyRepository");
		repositoryMap.put(Machine.class,"machineRepository");
		repositoryMap.put(MachineHistory.class,"machineHistoryRepository");
		repositoryMap.put(MachineSpecification.class,"machineSpecificationRepository");
		repositoryMap.put(MachineType.class,"machineTypeRepository");
		repositoryMap.put(ModuleDetail.class,"moduleRepository");
		repositoryMap.put(ReasonCategory.class,"reasonCategoryRepository");
		repositoryMap.put(ReasonList.class,"reasonListRepository");
		repositoryMap.put(RegExceptionalHoliday.class, "RegExceptionalHolidayRepo");
		repositoryMap.put(RegistrationCenter.class,"registrationCenterRepository");
		repositoryMap.put(RegistrationCenterHistory.class,"registrationCenterHistoryRepository");
		repositoryMap.put(RegistrationCenterType.class,"registrationCenterTypeRepository");
		repositoryMap.put(RegWorkingNonWorking.class, "workingDaysRepo");
		repositoryMap.put(Template.class,"templateRepository");
		repositoryMap.put(TemplateFileFormat.class,"templateFileFormatRepository");
		repositoryMap.put(TemplateType.class,"templateTypeRepository");
		repositoryMap.put(Title.class,"titleRepository");
		repositoryMap.put(UserDetails.class,"userDetailsRepository");
		repositoryMap.put(UserDetailsHistory.class,"userDetailsHistoryRepository");
		repositoryMap.put(ValidDocument.class,"validDocumentRepository");
		repositoryMap.put(Zone.class,"zoneRepository");
		repositoryMap.put(ZoneUser.class,"zoneUserRepository");
		repositoryMap.put(DaysOfWeek.class, "daysOfWeekRepo");
		repositoryMap.put(ZoneUserHistory.class,"zoneUserHistoryRepository");
		
    }
    public static String getRepo(Class<?> clzz) {
    	return repositoryMap.get(clzz);
    }


	private static void loadEntity() {
		
		entityMap.put("applicant_valid_document", ApplicantValidDocument.class);
		entityMap.put("appl_form_type", Application.class);
		entityMap.put("biometric_attribute", BiometricAttribute.class);
		entityMap.put("biometric_type",BiometricType.class );
		entityMap.put("blocklisted_words", BlocklistedWords.class);
		entityMap.put("daysofweek_list", DaysOfWeek.class);
		entityMap.put("device_master", Device.class);
		entityMap.put("device_master_h", DeviceHistory.class);
		entityMap.put("device_spec", DeviceSpecification.class);
		entityMap.put("device_type", DeviceType.class);
		entityMap.put("doc_category", DocumentCategory.class);
		entityMap.put("doc_type", DocumentType.class);
		entityMap.put("dynamic_field", DynamicField.class);
		//entityMap.put("reg_exceptional_holiday", ExceptionalHoliday.class);
		entityMap.put("gender", Gender.class);
		entityMap.put("loc_holiday", Holiday.class);
		entityMap.put("identity_schema", IdentitySchema.class);
		entityMap.put("id_type", IdType.class);
		entityMap.put("individual_type", IndividualType.class);
		entityMap.put("language", Language.class);
		entityMap.put("location", Location.class);
		entityMap.put("loc_hierarchy_list", LocationHierarchy.class);
		entityMap.put("machine_master", Machine.class);
		entityMap.put("machine_master_h", MachineHistory.class);
		entityMap.put("machine_spec", MachineSpecification.class);
		entityMap.put("machine_type", MachineType.class);
		entityMap.put("module_detail", ModuleDetail.class);
		entityMap.put("reason_category", ReasonCategory.class);
		entityMap.put("reason_list", ReasonList.class);
		entityMap.put("reg_exceptional_holiday", RegExceptionalHoliday.class);
		entityMap.put("registration_center", RegistrationCenter.class);
		entityMap.put("registration_center_h", RegistrationCenterHistory.class);
		entityMap.put("reg_center_type", RegistrationCenterType.class);
		entityMap.put("reg_working_nonworking", RegWorkingNonWorking.class);
		entityMap.put("schema_def", SchemaDefinition.class);
		entityMap.put("template", Template.class);
		entityMap.put("template_file_format", TemplateFileFormat.class);
		entityMap.put("template_type", TemplateType.class);
		entityMap.put("title", Title.class);
		entityMap.put("user_detail", UserDetails.class);
		entityMap.put("user_detail_h", UserDetailsHistory.class);
		entityMap.put("valid_document", ValidDocument.class);
		entityMap.put("zone", Zone.class);
		entityMap.put("zone_user", ZoneUser.class);
		entityMap.put("zone_user_h", ZoneUserHistory.class);
	}
	
	public static Class<?> getEntity(String tableName) {
		return entityMap.get(tableName);
	}
}
