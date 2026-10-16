/**
 * 
 */
package io.mosip.masterdata.service;

import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.masterdata.dto.FoundationalTrustProviderDto;
import io.mosip.masterdata.dto.FoundationalTrustProviderPutDto;
import io.mosip.masterdata.dto.getresponse.FoundationalTrustProviderResDto;

/**
 * @author Ramadurai Pandian
 *
 */
public interface FoundationalTrustProviderService {

	ResponseWrapper<FoundationalTrustProviderResDto> registerFoundationalTrustProvider(
			FoundationalTrustProviderDto request);

	ResponseWrapper<FoundationalTrustProviderResDto> updateFoundationalTrustProvider(
			FoundationalTrustProviderPutDto request);

}
