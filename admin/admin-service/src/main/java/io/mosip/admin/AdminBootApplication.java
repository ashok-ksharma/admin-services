package io.mosip.admin;

import java.util.concurrent.Executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.mosip.kernel.datamapper.orika.impl.DataMapperImpl;

/**
 * Entry point of the merged admin-services application.
 *
 * <p>
 * Datasource auto-configuration is switched off so that no auto-configured
 * {@code DataSource} can capture generic {@code spring.datasource.*} keys. Each database
 * gets an explicitly wired configuration instead: {@code mosip_master} via
 * {@link io.mosip.admin.config.MasterDataSourceConfig}, and {@code mosip_hotlist} via its
 * own configuration once hotlist is folded in.
 * </p>
 *
 * <p>
 * Note that {@code "io.mosip.kernel.dataaccess.*"} is deliberately absent from the
 * component scan. It used to pull in kernel's {@code HibernateDaoConfig}, whose
 * {@code io.mosip.*} entity/repository scanning cannot coexist with a second datasource;
 * {@code MasterDataSourceConfig} supersedes it. The kernel-dataaccess dependency itself is
 * still required for {@code HibernateRepositoryImpl}.
 * </p>
 *
 * <p>
 * Three things the retired {@code MasterDataBootApplication} carried are deliberately
 * <em>not</em> reproduced here. {@code @EnableCaching} and {@code @EnableScheduling} are
 * already declared by masterdata's own {@code CacheConfig}, which this scan picks up.
 * {@code @Import(HibernateDaoConfig.class)} is dropped for the reason given above - it
 * declares {@code @EnableJpaRepositories("io.mosip.*")} and
 * {@code setPackagesToScan("io.mosip.*")}, which would swallow hotlist's entities onto the
 * master datasource (merge plan risk R1). {@code MasterDataSourceConfig} already maps
 * masterdata's entities and repositories explicitly.
 * </p>
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@EnableAsync
@ComponentScan(value = {"io.mosip.kernel.auth.*","io.mosip.admin.*","io.mosip.commons.*",
		"${mosip.auth.adapter.impl.basepackage}", "io.mosip.kernel.idvalidator.rid.*","io.mosip.kernel.biometrics.*","io.mosip.kernel.authcodeflowproxy.*",
		// --- masterdata -------------------------------------
		// The eight entries below are exactly what the retired MasterDataBootApplication
		// scanned, minus "io.mosip.kernel.auth.*" which admin-service already lists.
		// masterdata's own beans, plus the kernel implementations behind the SPIs it
		// autowires: DataMapper, PublisherClient (WebSub), the machine/device/centre id
		// generators, and ApplicantType.
		"io.mosip.kernel.masterdata.*","io.mosip.kernel.core.datamapper.*",
		"io.mosip.kernel.core.websub.*","io.mosip.kernel.idgenerator.*",
		"io.mosip.kernel.websub.api.*","io.mosip.kernel.applicanttype.*",
		"io.mosip.kernel.core.idgenerator.*","io.mosip.kernel.core.logger.config"},
		// Carried over verbatim from MasterDataBootApplication. MapperConfig builds the
		// DataMapper it needs through the Orika builder, so the component-scanned
		// DataMapperImpl would be a second, unconfigured candidate for the same type.
		excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
				classes = { DataMapperImpl.class }) })
public class AdminBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminBootApplication.class, args);
	}

	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(20);
		executor.setMaxPoolSize(40);
		executor.setThreadNamePrefix("Admin-Async-Thread-");
		executor.initialize();
		return executor;
	}

}
