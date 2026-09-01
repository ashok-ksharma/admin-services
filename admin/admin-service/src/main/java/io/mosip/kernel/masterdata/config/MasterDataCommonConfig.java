package io.mosip.kernel.masterdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import io.mosip.kernel.core.masterdata.util.model.Node;
import io.mosip.kernel.core.masterdata.util.spi.UBtree;
import io.mosip.kernel.masterdata.entity.Location;
import io.mosip.kernel.masterdata.entity.Zone;
import io.mosip.kernel.masterdata.utils.DefaultSort;

/**
 * Config class with beans for modelmapper and request logging
 *
 * @author Dharmesh Khandelwal
 * @author Urvil Joshi
 * @since 1.0.0
 *
 */
/*
 * Renamed from CommonConfig during the admin-services merge. Both this class and
 * io.mosip.admin.config.CommonConfig would otherwise register under the bean name
 * "commonConfig" and the application would fail to start with
 * ConflictingBeanDefinitionException.
 *
 * Four beans were dropped here rather than renamed, because admin-service already
 * contributes an identical one and a second copy would take effect on every request in
 * the merged application:
 *
 *   logFilter, getReqResFilter, registerReqResFilter - admin-service's CommonConfig
 *       declares the same three. Its io.mosip.admin.httpfilter.ReqResFilter is
 *       byte-identical to the masterdata copy that was deleted with them (both 58 lines;
 *       the only difference was which LoggerConfiguration they imported, and those two
 *       classes are identical too). Both registrations use order 2 with no URL pattern,
 *       so a single one already covers every request in the merged application - exactly
 *       the coverage each service had for its own requests before the merge. Keeping both
 *       would wrap and log each request twice.
 *
 *   auditUtil - it duplicated the @Component on io.mosip.kernel.masterdata.utils.AuditUtil,
 *       both claiming the bean name "auditUtil". A @Bean method silently overrides a
 *       component-scanned definition of the same name, which is how masterdata worked
 *       standalone; in the merged application that override would have landed on
 *       admin-service's own AuditUtil instead. AuditUtil is now registered once, as
 *       @Component("masterdataAuditUtil"), and every injection of it is by type.
 *
 * The remaining beans are masterdata-specific - zoneTree and locationTree are typed on
 * masterdata entities - so this class is kept alongside admin's rather than merged into it.
 */
@Configuration
@EnableAspectJAutoProxy
public class MasterDataCommonConfig {

	@Bean(name = "zoneTree")
	public UBtree<Zone> zoneTree() {
		return zone -> new Node<>(zone.getCode(), zone, zone.getParentZoneCode());
	}

	@Bean(name = "locationTree")
	public UBtree<Location> locationTree() {
		return location -> new Node<>(location.getCode(), location, location.getParentLocCode());
	}

	@Bean
	public DefaultSort defaultSort() {
		return new DefaultSort();
	}

}
