package io.mosip.kernel.masterdata.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import io.mosip.admin.config.CommonConfig;
import io.mosip.admin.config.MasterDataSourceConfig;
import io.mosip.admin.packetstatusupdater.util.RestClient;
import io.mosip.kernel.masterdata.test.config.TestConfig;
import io.mosip.kernel.masterdata.test.config.TestSecurityConfig;

/**
 * Main class of Sync handler Application.
 *
 * @author Abhishek Kumar
 * @since 1.0.0
 */

// Mirrors AdminBootApplication's single-datasource setup: MasterDataSourceConfig is the only
// dataSource/entityManagerFactory, so kernel's HibernateDaoConfig is no longer imported and
// datasource auto-configuration is excluded. RestClient lives in io.mosip.admin, outside this
// scan, and is needed by PacketWorkflowActionServiceImpl. CommonConfig registers ReqResFilter
// (ContentCachingRequestWrapper) for every request, as it does in the merged application;
// masterdata no longer registers its own copy of that filter, and its exception handler
// and response-body advice read the cached request body.
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@ComponentScan(basePackages = {"io.mosip.kernel.masterdata.*","io.mosip.kernel.core.datamapper.*",
		"io.mosip.kernel.core.websub.*","io.mosip.kernel.idgenerator.*"
		,"io.mosip.kernel.websub.api.*","io.mosip.kernel.applicanttype.*","io.mosip.kernel.core.idgenerator.*"}
)
@Import(value = {TestConfig.class, TestSecurityConfig.class, MasterDataSourceConfig.class, RestClient.class,
		CommonConfig.class})
public class TestBootApplication {

	/**
	 * Function to run the Master-Data-Service application
	 * 
	 * @param args The arguments to pass will executing the main function
	 */
	public static void main(String[] args) {
		SpringApplication.run(TestBootApplication.class, args);
	}

}