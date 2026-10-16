package io.mosip.masterdata.service;

import java.util.List;

import jakarta.validation.Valid;

import io.mosip.masterdata.dto.PageDto;
import io.mosip.masterdata.dto.SearchDtoWithoutLangCode;
import io.mosip.masterdata.dto.UserDetailsCenterMapping;
import io.mosip.masterdata.dto.UserDetailsDto;
import io.mosip.masterdata.dto.UserDetailsGetExtnDto;
import io.mosip.masterdata.dto.UserDetailsPutDto;
import io.mosip.masterdata.dto.UserDetailsPutReqDto;
import io.mosip.masterdata.dto.UsersDto;
import io.mosip.masterdata.dto.getresponse.StatusResponseDto;
import io.mosip.masterdata.dto.getresponse.extn.UserCenterMappingExtnDto;
import io.mosip.masterdata.dto.getresponse.extn.UserDetailsExtnDto;
import io.mosip.masterdata.dto.postresponse.IdResponseDto;
import io.mosip.masterdata.dto.response.PageResponseDto;
import io.mosip.masterdata.entity.id.RegistrationCenterUserID;
import io.mosip.masterdata.exception.DataNotFoundException;
import io.mosip.masterdata.exception.MasterDataServiceException;
import io.mosip.masterdata.exception.RequestException;

/**
 * @author Sidhant Agarwal
 * @since 1.0.0
 *
 */
public interface UserDetailsService {
	/**
	 * This abstract method to fetch users details for given User Id. Note: the user id should be the username of the IAM system.
	 * 
	 * @param id       User Id given by user
	 * @return UserResponseDto User Detail for given user id and language
	 *         code
	 * @throws MasterDataServiceException if any error occurs while retrieving
	 *                                    User Details
	 * @throws DataNotFoundException      if no User found
	 *
	 */
	public UserDetailsGetExtnDto getUser(String id);

	/**
	 * This method provides with all templates.
	 * 
	 * @param pageNumber the page number
	 * @param pageSize   the size of each page
	 * @param sortBy     the attributes by which it should be ordered
	 * @param direction    the order to be used
	 * 
	 * @return the response i.e. pages containing the templates.
	 */
	public PageDto<UserDetailsExtnDto> getUsers(int pageNumber, int pageSize, String sortBy, String direction);


	/**
	 * Abstract method to delete User Details to the Database
	 * 
	 * @param id user id
	 * 
	 * @return IdResponseDto returning user id which is updated successfully
	 *         {@link IdResponseDto}
	 * @throws RequestException           if User not Found
	 * 
	 * @throws MasterDataServiceException if any error occurred while updating
	 *                                    User
	 * 
	 */
	public IdResponseDto deleteUser(String id);

	/**
	 * Fetch all Users which are mapped with the given registration center
	 * 
	 * @throws MasterDataServiceException if any error occurred while updating
	 *                                    User
	 * 
	 * @param regCenterId Registration center id as String
	 * @return UserDetailsResponseDto response object which contain the list
	 *         of users those are mapped with the given registration center
	 *         {@link RegistrationCenterUserID}
	 */
	public List<UserDetailsExtnDto> getUsersByRegistrationCenter(String regCenterId, int pageNumber, int pageSize, String sortBy, String orderBy);

	
	/**
	 * Abstract method to create a user in the User Details database.
	 * 
	 * @param userDetailsDto user details
	 * 
	 * @return IdAndLanguageCodeID updated user id and language code.
	 * 
	 */
	public UserDetailsCenterMapping createUser(UserDetailsDto userDetailsDto);

	/**
	 * Abstract method to update User Details to the database
	 * 
	 * @param user User Put Request DTO
	 * 
	 * @return UserDetailsDto returning updated user {@link UserDetailsDto}
	 * 
	 * @throws MasterDataServiceException if any error occurred while updating
	 *                                    User
	 */
	public UserDetailsPutDto updateUser(UserDetailsPutReqDto user);

	/**
	 * Abstract method to get all users from the IAM.
	 * 
	 * Will call auth service to get all users
	 * 
	 * @param roleName
	 * @return
	 */

	public UsersDto getUsers(String roleName,int pageStart, int pageFetch,
			String firstName, String lastName, String username);
	
	/**
	 * 
	 * @param searchDtoWithoutLangCode
	 * @return
	 */
	public PageResponseDto<UserDetailsExtnDto> searchUserDetails(SearchDtoWithoutLangCode searchDtoWithoutLangCode);

	public StatusResponseDto updateUserStatus(String id, @Valid boolean isActive);

	public PageResponseDto<UserCenterMappingExtnDto> searchUserCenterMappingDetails(SearchDtoWithoutLangCode request);

}
