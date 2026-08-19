package io.mosip.admin;

import io.mosip.commons.packet.impl.OnlinePacketCryptoServiceImpl;
import io.mosip.commons.packet.keeper.PacketKeeper;

import javax.validation.Validator;


import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.*;

// Mirrors AdminBootApplication: "io.mosip.kernel.dataaccess.*" is dropped so kernel's
// HibernateDaoConfig cannot register a competing dataSource/entityManagerFactory alongside
// MasterDataSourceConfig, and datasource auto-configuration is excluded for the same reason.
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
@ComponentScan(value = {"io.mosip.admin.*","io.mosip.commons.*","io.mosip.kernel.biometrics.*",
		 "io.mosip.kernel.idvalidator.rid.*"},
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASPECTJ,
		pattern = "io.mosip.kernel.lkeymanager.repository.*"))
public class TestBootApplication {
	public static void main(String[] args) {
		SpringApplication.run(TestBootApplication.class, args);
	}

	@Bean
	@Primary
	public OnlinePacketCryptoServiceImpl onlineCrypto() {
		return Mockito.mock(OnlinePacketCryptoServiceImpl.class);
	}

	@Bean
	@Primary
	public PacketKeeper packetKeeper() {
		return Mockito.mock(PacketKeeper.class);
	}

	@Bean
	public Validator validator() {
		return Mockito.mock(Validator.class);
	}
}
