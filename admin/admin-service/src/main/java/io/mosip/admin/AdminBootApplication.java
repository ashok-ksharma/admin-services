package io.mosip.admin;

import java.util.concurrent.Executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@EnableAsync
@ComponentScan(value = {"io.mosip.kernel.auth.*","io.mosip.admin.*","io.mosip.commons.*",
		"${mosip.auth.adapter.impl.basepackage}", "io.mosip.kernel.idvalidator.rid.*","io.mosip.kernel.biometrics.*","io.mosip.kernel.authcodeflowproxy.*"})
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
