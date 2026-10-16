package io.mosip.mergedadmin;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.BeforeClass;
import org.junit.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import io.mosip.masterdata.service.impl.PacketWorkflowActionServiceImpl;

/**
 * Enforces the package boundaries described in {@code io.mosip.mergedadmin.package-info}.
 * Checks compiled production classes only (test classes are not imported).
 *
 * <p>
 * Compile-time constants (for example {@code ApiPathPrefixConfig.MASTERDATA_PREFIX}) are inlined by
 * the compiler, so a class that only reads such constants has no dependency in its bytecode and is
 * not reported here.
 * </p>
 */
public class PackageBoundaryTest {

	private static JavaClasses productionClasses;

	@BeforeClass
	public static void importClasses() {
		productionClasses = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages("io.mosip.mergedadmin", "io.mosip.admin", "io.mosip.masterdata");
	}

	@Test
	public void mergedAdminDoesNotDependOnServices() {
		noClasses().that().resideInAPackage("io.mosip.mergedadmin..").should().dependOnClassesThat()
				.resideInAnyPackage("io.mosip.admin..", "io.mosip.masterdata..").check(productionClasses);
	}

	/**
	 * Allowed exception, to be evaluated with the other cross-service uses:
	 * PacketWorkflowActionServiceImpl calls registration processor through admin's RestClient and
	 * ApiName.
	 */
	@Test
	public void masterdataDoesNotDependOnAdmin() {
		noClasses().that().resideInAPackage("io.mosip.masterdata..").and()
				.doNotBelongToAnyOf(PacketWorkflowActionServiceImpl.class).should().dependOnClassesThat()
				.resideInAPackage("io.mosip.admin..").check(productionClasses);
	}
}
