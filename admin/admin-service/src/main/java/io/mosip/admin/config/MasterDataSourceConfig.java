package io.mosip.admin.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
 * stay on its own {@code mosip_hotlist} datasource.
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
		basePackages = { "io.mosip.kernel.masterdata.repository", "io.mosip.admin.bulkdataupload.repositories",
				"io.mosip.kernel.idgenerator.machineid.repository",
				"io.mosip.kernel.idgenerator.regcenterid.repository" },
		entityManagerFactoryRef = "masterEntityManagerFactory",
		transactionManagerRef = "masterTxManager",
		repositoryBaseClass = HibernateRepositoryImpl.class)
public class MasterDataSourceConfig {

	private static final Logger logger = LoggerFactory.getLogger(MasterDataSourceConfig.class);

	/**
	 * Entity packages mapped onto the mosip_master datasource.
	 *
	 * <p>
	 * masterdata's package holds the <em>canonical</em> entity set: admin's ~59 duplicate
	 * classes were deleted and its repositories repointed here, so exactly one {@code @Entity}
	 * now maps each {@code master.*} table. {@code io.mosip.admin.bulkdataupload.entity} is
	 * still listed because 14 admin-only classes remain there - the bulk-upload transaction
	 * log, the applicant login detail, and the {@code reg_center_*} mapping entities.
	 * </p>
	 *
	 * <p>
	 * {@code io.mosip.kernel.idgenerator.machineid.entity} and
	 * {@code io.mosip.kernel.idgenerator.regcenterid.entity} are needed because
	 * {@code io.mosip.kernel.idgenerator.*} is in the component scan: that scan reaches each
	 * library's {@code impl} service (e.g. {@code MachineIdGeneratorImpl}), which injects a
	 * JPA repository from the sibling {@code repository} package. Without these two packages
	 * here too, the application fails to start with "No qualifying bean of type
	 * '...MachineIdRepository' available".
	 * </p>
	 *
	 * <p>
	 * Pinned to these packages rather than {@code io.mosip.*}: a wildcard would swallow
	 * hotlist's entities, which must stay on their own {@code mosip_hotlist} datasource.
	 * </p>
	 */
	static final String[] MASTER_ENTITY_PACKAGES = { "io.mosip.kernel.masterdata.entity",
			"io.mosip.admin.bulkdataupload.entity", "io.mosip.kernel.idgenerator.machineid.entity",
			"io.mosip.kernel.idgenerator.regcenterid.entity" };

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
	 * Open-session-in-view for the {@code mosip_master} persistence unit, limited to
	 * {@link #OPEN_SESSION_IN_VIEW_PATHS}.
	 *
	 * <p>
	 * <b>Why it exists.</b> admin-service and kernel-masterdata-service each ran as a plain
	 * {@code @SpringBootApplication}, so Spring Boot's {@code HibernateJpaAutoConfiguration}
	 * registered this interceptor for every request ({@code spring.jpa.open-in-view} defaults
	 * to {@code true}). The combined application excludes that auto-configuration and builds
	 * its persistence setup explicitly in this class, which removes the interceptor with it.
	 * Without it, code that reads a lazy association after the repository call has returned
	 * fails with {@code LazyInitializationException} - e.g. {@code GET
	 * /v1/masterdata/packetrejectionreasons} reading {@code ReasonCategory.reasonList}. It is
	 * declared here, rather than by re-enabling the auto-configuration, so that it covers only
	 * the services that relied on it.
	 * </p>
	 *
	 * <p>
	 * <b>What it does to a request it covers.</b> One {@code EntityManager} is opened when
	 * the request starts and closed only after the response has been written:
	 * </p>
	 * <ul>
	 * <li>lazy associations can be read anywhere in the request, including controllers,
	 * DTO mapping and JSON serialization;</li>
	 * <li>once the request first touches the database, the JDBC connection is held until the
	 * response is fully written (Hibernate acquires it lazily, then keeps it for the
	 * session), so slow or large responses keep a pool connection busy for longer;</li>
	 * <li>every repository call in the request shares one session: the same row read twice
	 * returns the same cached instance;</li>
	 * <li>an entity loaded earlier in the request and modified outside a transaction is
	 * flushed if a {@code @Transactional} method runs later in the same request.</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Before adding a path to {@link #OPEN_SESSION_IN_VIEW_PATHS}</b> - for example when
	 * another service is combined into this application - check that service against the
	 * points above. Adding a path changes that service's behaviour; decide per service:
	 * </p>
	 * <ul>
	 * <li><b>Did it run with open-session-in-view on its own?</b> kernel-syncdata-service did
	 * <b>not</b>: its boot class excludes {@code HibernateJpaAutoConfiguration}.</li>
	 * <li><b>Does it read lazy associations outside a transaction?</b> kernel-syncdata-service
	 * does not: its lazy fields are never navigated, related data is loaded as separate flat
	 * lists, many queries are native SQL or DTO projections, and its data loading runs in
	 * {@code @Async} helpers - on other threads, which this interceptor never covers.</li>
	 * <li><b>Does it hit the database on the request thread?</b> Each such request would hold
	 * its connection until the response is written. kernel-syncdata-service does (repository
	 * calls in {@code SyncAuthTokenServiceImpl}, {@code SyncMasterDataServiceImpl} and
	 * {@code SyncUserDetailsServiceImpl}) and returns large sync payloads, so covering
	 * {@code /v1/syncdata/**} would raise connection hold time and pool pressure for no
	 * functional gain.</li>
	 * <li><b>Is it on this persistence unit at all?</b> This interceptor only covers
	 * {@code masterEntityManagerFactory}. hotlist-service uses its own {@code mosip_hotlist}
	 * persistence unit (and ran with Spring Boot's open-session-in-view on its own); adding
	 * {@code /v1/hotlist/**} here would not help it - it needs its own interceptor bound to
	 * its own entity manager factory.</li>
	 * </ul>
	 */
	@Bean
	public OpenEntityManagerInViewInterceptor masterOpenEntityManagerInViewInterceptor(
			@Qualifier("masterEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
		interceptor.setEntityManagerFactory(entityManagerFactory);
		return interceptor;
	}

	/**
	 * Paths covered by {@link #masterOpenEntityManagerInViewInterceptor}: admin-service and
	 * kernel-masterdata-service, which relied on open-session-in-view when they ran on their
	 * own. Read its javadoc before adding one.
	 */
	static final String[] OPEN_SESSION_IN_VIEW_PATHS = { ApiPathPrefixConfig.ADMIN_PREFIX + "/**",
			ApiPathPrefixConfig.MASTERDATA_PREFIX + "/**" };

	@Bean
	public WebMvcConfigurer masterOpenEntityManagerInViewConfigurer(
			OpenEntityManagerInViewInterceptor masterOpenEntityManagerInViewInterceptor) {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addWebRequestInterceptor(masterOpenEntityManagerInViewInterceptor)
						.addPathPatterns(OPEN_SESSION_IN_VIEW_PATHS);
			}
		};
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
	 * {@code hibernate.empty.interceptor} property and instantiated by class name (e.g.
	 * masterdata's {@code MasterDataInterceptor}).
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
