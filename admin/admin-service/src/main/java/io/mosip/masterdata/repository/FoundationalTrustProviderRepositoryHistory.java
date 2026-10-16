/**
 * 
 */
package io.mosip.masterdata.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.masterdata.dto.FoundationalTrustProviderDto;
import io.mosip.masterdata.entity.FoundationalTrustProvider;
import io.mosip.masterdata.entity.FoundationalTrustProviderHistory;

/**
 * @author Ramadurai Pandian
 *
 */
@Repository
public interface FoundationalTrustProviderRepositoryHistory
		extends BaseRepository<FoundationalTrustProviderHistory, String> {

}
