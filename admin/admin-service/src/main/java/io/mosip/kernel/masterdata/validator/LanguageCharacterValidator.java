package io.mosip.kernel.masterdata.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;

import lombok.Data;

//TODO: - rename this class with a more relevant name

@Data
public class LanguageCharacterValidator implements ConstraintValidator<AlphabeticValidator, String> {
	
	/*
	 * The ':[^a-zA-Z]' fallback is carried over from admin-service's copy of this validator,
	 * deleted when the entity sets were consolidated. masterdata resolves this key from
	 * kernel-default.properties (spring.cloud.config.name=kernel), but the merged application
	 * runs under config name 'admin', where it is not yet defined - without the fallback the
	 * placeholder does not resolve and blocklisted-word validation stops behaving as it did
	 * pre-merge.
	 */
	@Value("${mosip.kernel.masterdata.name.validate.regex:[^a-zA-Z]}")
	private String allowedCharactersRegex;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (allowedCharactersRegex == null || allowedCharactersRegex.isBlank()) {
			/*
			 * Ported from admin-service's copy of this validator, which was deleted when the
			 * entity sets were consolidated. Hibernate runs bean validation a second time at
			 * flush (jakarta.persistence.validation.mode defaults to AUTO), reached from
			 * RepositoryListItemWriter.doInvoke() during bulk upload, and on that pass the
			 * regex can be unresolved. Without this guard Pattern.compile(null) throws.
			 */
			return true;
		}
		if (null != value && !value.isEmpty()) {
			Pattern p = Pattern.compile(allowedCharactersRegex, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(value.trim());
			return !(m.find());
		}
		return true;
	}
}
