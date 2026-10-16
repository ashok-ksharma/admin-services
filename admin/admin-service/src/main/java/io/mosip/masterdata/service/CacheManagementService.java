package io.mosip.masterdata.service;

import io.mosip.masterdata.utils.CacheName;

/**
 * @author GOVINDARAJ VELU
 *
 */
public interface CacheManagementService {

	void clearCacheByCacheName(CacheName cacheName);
	
	void clearCache();
}
