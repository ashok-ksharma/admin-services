package io.mosip.kernel.masterdata.validator;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class FieldValidator implements ConstraintValidator<DynamicFieldValidator, JsonNode> {
    @Value("${mosip.kernel.masterdata.code.validate.regex}")
    private  String allowedCodeCharactersRegex;

    /*
     * Default regex for dynamic-field values. The key mosip.kernel.masterdata.value.validate.regex
     * is not defined in any config-server file (admin-default, kernel-default or
     * application-default, in mosip-config or mosip-config-test). Without this default the
     * placeholder cannot be resolved, and this ConstraintValidator - created lazily on first
     * use - fails with "Could not resolve placeholder" the first time a DynamicFieldDto is
     * validated. Same defensive pattern as LanguageCharacterValidator. The intended production
     * value still needs confirming.
     */
    @Value("${mosip.kernel.masterdata.value.validate.regex:[^a-z0-9]}")
    private String allowedValueCharactersRegex;

    private Pattern codePattern;
    private Pattern valuePattern;

    @Override
    public void initialize(DynamicFieldValidator constraintAnnotation) {
        codePattern = Pattern.compile(allowedCodeCharactersRegex, Pattern.CASE_INSENSITIVE);
        valuePattern = Pattern.compile(allowedValueCharactersRegex, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean isValid(JsonNode jsonValue, ConstraintValidatorContext context) {
        JsonNode val = jsonValue.get("value");
        String value = val.asText();
        val = jsonValue.get("code");
        String code = val.asText();
        if (value == null || value.isEmpty() || code == null || code.isEmpty()) {
            return false;
        }
        Matcher mCode = codePattern.matcher(code.trim());
        Matcher mValue = valuePattern.matcher(value.trim());
        return (mCode.find() || mValue.find()) ? false : true;
    }
}