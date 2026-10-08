package io.mosip.kernel.masterdata.config;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Configuration class for LocalDate, LocalTime LocalDateTime.
 * 
 * @author Sagar Mahapatra
 * @author Urvil Joshi
 *
 */
@Configuration
public class LocalDateLocalTimeConfig {
	public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	public static final DateTimeFormatter UTC_DATE_TIME_FORMAT = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	/*
	 * Merged application: this is the single @Primary ObjectMapper, so Spring Boot's
	 * auto-configured one backs off (@ConditionalOnMissingBean) and admin-service's
	 * controllers and its ten objectMapper.readValue(...) call sites now use this bean too.
	 *
	 * That matters for one Jackson default. This mapper is built with new ObjectMapper()
	 * rather than through Boot's Jackson2ObjectMapperBuilder, and Jackson's own default is
	 * to THROW on an unknown property, whereas Boot's builder disables that. Left alone,
	 * admin-service would start rejecting responses from regproc, digital-card and
	 * packet-manager the moment any of them adds a field - where before the merge the
	 * field was ignored. It is therefore disabled explicitly below, restoring
	 * admin-service's pre-merge behaviour.
	 *
	 * The trade-off, recorded deliberately: masterdata was strict before the merge, and its
	 * ApiExceptionHandler turned an unknown property into REQUEST_DATA_NOT_VALID via
	 * onHttpMessageNotReadable (UnrecognizedPropertyException extends
	 * MismatchedInputException). Requests carrying a misspelled or extra field are now
	 * accepted with the field ignored instead of reported. Type mismatches and missing
	 * input are unaffected - those still arrive as InvalidFormatException. No test in
	 * either suite asserts the old behaviour. If tests fail on deserialisation, revisit
	 * this attribute, and prefer @JsonIgnoreProperties(ignoreUnknown = true) on the affected
	 * admin DTOs over flipping this flag back.
	 *
	 * Serialization is unaffected: the JavaTimeModule below pins LocalDate, LocalTime and
	 * LocalDateTime by type for every class in the application, so admin-service's DTOs
	 * inherit the same formats without needing per-field @JsonFormat.
	 */
	@Bean
	@Primary
	public ObjectMapper serializingObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		JavaTimeModule javaTimeModule = new JavaTimeModule();
		javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer());
		javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer());
		javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer());
		javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer());
		javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
		javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
		objectMapper.registerModule(javaTimeModule);
		return objectMapper;
	}

	public static class LocalTimeDeserializer extends JsonDeserializer<LocalTime> {
		@Override
		public LocalTime deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
			return LocalTime.parse(jsonParser.getValueAsString(), TIME_FORMAT);
		}

	}

	public static class LocalTimeSerializer extends JsonSerializer<LocalTime> {
		@Override
		public void serialize(LocalTime localTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
				throws IOException {
			jsonGenerator.writeString(localTime.format(TIME_FORMAT));
		}
	}

	public static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
		@Override
		public LocalDate deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
			return LocalDate.parse(jsonParser.getValueAsString(), DATE_FORMAT);
		}

	}

	public static class LocalDateSerializer extends JsonSerializer<LocalDate> {
		@Override
		public void serialize(LocalDate localDate, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
				throws IOException {
			jsonGenerator.writeString(localDate.format(DATE_FORMAT));
		}
	}

	public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
		@Override
		public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator,
				SerializerProvider serializerProvider) throws IOException {
			jsonGenerator.writeString(localDateTime.format(UTC_DATE_TIME_FORMAT));
		}
	}

	public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
		@Override
		public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
			return LocalDateTime.parse(jsonParser.getValueAsString(), UTC_DATE_TIME_FORMAT);
		}
	}
}
