package io.mosip.admin.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.mosip.kernel.dataaccess.hibernate.constant.HibernatePersistenceConstant;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;

/**
 * Persistence configuration for the <code>mosip_master</code> database.
 *
 * <p>
 * This replaces {@code io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig},
 * which the pre-merge admin-service picked up through the
 * {@code "io.mosip.kernel.dataaccess.*"} component-scan entry. That class is unusable in
 * the merged application because it declares
 * {@code @EnableJpaRepositories(basePackages = "io.mosip.*")} and
 * {@code setPackagesToScan("io.mosip.*")}: a single persistence unit would swallow every
 * {@code io.mosip} entity and repository on the classpath, including hotlist's, which must
 * stay on its own {@code mosip_hotlist} datasource (risk R1 of the merge plan).
 * </p>
 *
 * <p>
 * Everything else is a faithful copy of {@code HibernateDaoConfig}: the same property keys
 * ({@code javax.persistence.jdbc.*}, {@code hikari.*}, {@code hibernate.*}), the same
 * defaults, and the same {@link HibernateRepositoryImpl} repository base class that MOSIP's
 * {@code BaseRepository} depends on. Only the scanned packages are narrowed, and the beans
 * are given explicit {@code master*} names so the hotlist datasource can be added alongside
 * them without a name clash.
 * </p>
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		basePackages = { "io.mosip.admin.bulkdataupload.repositories" },
		entityManagerFactoryRef = "masterEntityManagerFactory",
		transactionManagerRef = "masterTxManager",
		repositoryBaseClass = HibernateRepositoryImpl.class)
public class MasterDataSourceConfig {

	private static final Logger logger = LoggerFactory.getLogger(MasterDataSourceConfig.class);

	/** Entity packages mapped onto the mosip_master datasource.*/
	static final String[] MASTER_ENTITY_PACKAGES = { "io.mosip.admin.bulkdataupload.entity" };

	@Autowired
	private Environment environment;

	@Value("${hikari.maximumPoolSize:25}")
	private int maximumPoolSize;

	@Value("${hikari.validationTimeout:3000}")
	private int validationTimeout;

	@Value("${hikari.connectionTimeout:60000}")
	private int connectionTimeout;

	@Value("${hikari.idleTimeout:200000}")
	private int idleTimeout;

	@Value("${hikari.minimumIdle:0}")
	private int minimumIdle;

	@Bean
	@Primary
	public DataSource masterDataSource() {
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setDriverClassName(environment.getProperty(HibernatePersistenceConstant.JDBC_DRIVER));
		hikariConfig.setJdbcUrl(environment.getProperty(HibernatePersistenceConstant.JDBC_URL));
		hikariConfig.setUsername(environment.getProperty(HibernatePersistenceConstant.JDBC_USER));
		hikariConfig.setPassword(environment.getProperty(HibernatePersistenceConstant.JDBC_PASS));
		if (environment.containsProperty(HibernatePersistenceConstant.JDBC_SCHEMA)) {
			hikariConfig.setSchema(environment.getProperty(HibernatePersistenceConstant.JDBC_SCHEMA));
		}
		hikariConfig.setMaximumPoolSize(maximumPoolSize);
		hikariConfig.setValidationTimeout(validationTimeout);
		hikariConfig.setConnectionTimeout(connectionTimeout);
		hikariConfig.setIdleTimeout(idleTimeout);
		hikariConfig.setMinimumIdle(minimumIdle);
		return new HikariDataSource(hikariConfig);
	}

	@Bean
	@Primary
	public LocalContainerEntityManagerFactoryBean masterEntityManagerFactory() {
		LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactory.setDataSource(masterDataSource());
		entityManagerFactory.setPackagesToScan(MASTER_ENTITY_PACKAGES);
		entityManagerFactory.setPersistenceUnitName("master");
		entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		entityManagerFactory.setJpaPropertyMap(masterJpaProperties());
		return entityManagerFactory;
	}

	@Bean
	@Primary
	public PlatformTransactionManager masterTxManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager transactionManager = new JpaTransactionManager();
		transactionManager.setEntityManagerFactory(entityManagerFactory);
		return transactionManager;
	}

	/**
	 * Same keys and same defaults as {@code HibernateDaoConfig.jpaProperties()}, so the
	 * existing {@code hibernate.*} configuration keeps behaving identically.
	 */
	private Map<String, Object> masterJpaProperties() {
		HashMap<String, Object> jpaProperties = new HashMap<>();
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_HBM2DDL_AUTO,
				HibernatePersistenceConstant.UPDATE);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_DIALECT,
				HibernatePersistenceConstant.MY_SQL5_DIALECT);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_SHOW_SQL,
				HibernatePersistenceConstant.TRUE);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_FORMAT_SQL,
				HibernatePersistenceConstant.TRUE);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_CONNECTION_CHAR_SET,
				HibernatePersistenceConstant.UTF8);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE,
				HibernatePersistenceConstant.FALSE);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_CACHE_USE_QUERY_CACHE,
				HibernatePersistenceConstant.FALSE);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES,
				HibernatePersistenceConstant.FALSE);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_GENERATE_STATISTICS,
				HibernatePersistenceConstant.FALSE);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_NON_CONTEXTUAL_CREATION,
				HibernatePersistenceConstant.FALSE);
		putProperty(jpaProperties, HibernatePersistenceConstant.HIBERNATE_CURRENT_SESSION_CONTEXT,
				HibernatePersistenceConstant.JTA);
		addInterceptor(jpaProperties);
		return jpaProperties;
	}

	private void putProperty(HashMap<String, Object> jpaProperties, String property, String defaultValue) {
		jpaProperties.put(property, environment.getProperty(property, defaultValue));
	}

	/**
	 * Mirrors {@code HibernateDaoConfig}: the interceptor is opt-in via the
	 * {@code hibernate.empty.interceptor} property and instantiated by class name. Step 2
	 * attaches masterdata's {@code MasterDataInterceptor} here.
	 */
	private void addInterceptor(HashMap<String, Object> jpaProperties) {
		String interceptorClassName = environment.getProperty(HibernatePersistenceConstant.EMPTY_INTERCEPTOR);
		if (interceptorClassName == null || interceptorClassName.isBlank()) {
			return;
		}
		try {
			jpaProperties.put(HibernatePersistenceConstant.HIBERNATE_EJB_INTERCEPTOR,
					Class.forName(interceptorClassName).asSubclass(Interceptor.class).getDeclaredConstructor()
							.newInstance());
		} catch (ReflectiveOperationException e) {
			logger.error("Error while configuring Interceptor.", e);
		}
	}
}
