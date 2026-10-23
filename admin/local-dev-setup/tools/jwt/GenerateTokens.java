import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Generates the static JWTs and the WireMock IAM stubs for the local stack.
 *
 * <p>Run from {@code local-dev-setup/} with JDK 21, no build step:
 * <pre>
 *   java tools/jwt/GenerateTokens.java            # reuse keys/jwt-signing-key.pem (create it if missing)
 *   java tools/jwt/GenerateTokens.java --new-key  # rotate the signing key first
 * </pre>
 *
 * <p>Reads {@code tools/jwt/tokens.properties} and writes:
 * <ul>
 * <li>{@code deps/wiremock/mappings/keycloak-authmanager.json} - JWKS, client token and user login stubs</li>
 * </ul>
 */
public class GenerateTokens {

	private static final Path PROPERTIES = Path.of("tools", "jwt", "tokens.properties");
	private static final Path KEY_FILE = Path.of("keys", "jwt-signing-key.pem");
	private static final Path MAPPING_FILE = Path.of("deps", "wiremock", "mappings", "keycloak-authmanager.json");

	// expires_in / expiryTime are read as int by some clients; the exp claim carries the real validity.
	private static final long MAX_EXPIRES_IN = Integer.MAX_VALUE;

	private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

	record TokenDef(String name, String type, String clientId, String username, List<String> roles) {
	}

	record Token(TokenDef def, String jwt) {
	}

	public static void main(String[] args) throws Exception {
		if (!Files.exists(PROPERTIES)) {
			throw new IllegalStateException("Run from local-dev-setup/: " + PROPERTIES + " not found");
		}
		boolean newKey = args.length > 0 && "--new-key".equals(args[0]);

		Properties props = new Properties();
		try (Reader reader = Files.newBufferedReader(PROPERTIES, StandardCharsets.UTF_8)) {
			props.load(reader);
		}
		String issuer = required(props, "issuer");
		String kid = required(props, "kid");
		long years = Long.parseLong(required(props, "validity.years"));

		RSAPrivateCrtKey key = loadOrCreateKey(newKey);
		Instant iat = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		// ChronoUnit.YEARS is not supported on Instant; 365.2425 days per Gregorian year.
		Instant exp = iat.plusSeconds(years * 31_556_952L);
		long expiresIn = Math.min(exp.getEpochSecond() - iat.getEpochSecond(), MAX_EXPIRES_IN);

		List<Token> tokens = new ArrayList<>();
		for (TokenDef def : readDefinitions(props)) {
			tokens.add(new Token(def, sign(key, kid, payload(def, issuer, iat, exp))));
		}

		Files.createDirectories(MAPPING_FILE.getParent());
		Files.writeString(MAPPING_FILE, mappings(tokens, key, kid, expiresIn));

		System.out.println("Signing key : " + KEY_FILE + (newKey ? " (new)" : ""));
		System.out.println("Expires     : " + exp);
		System.out.println("Stubs       : " + MAPPING_FILE);
		for (Token token : tokens) {
			TokenDef def = token.def();
			System.out.printf("Token       : %s  (%s %s)%n", def.name(), def.type(),
					"user".equals(def.type()) ? def.username() : def.clientId());
		}
	}

	private static List<TokenDef> readDefinitions(Properties props) {
		TreeSet<String> names = new TreeSet<>();
		for (String key : props.stringPropertyNames()) {
			if (key.startsWith("token.")) {
				names.add(key.substring("token.".length(), key.lastIndexOf('.')));
			}
		}
		List<TokenDef> defs = new ArrayList<>();
		for (String name : names) {
			String prefix = "token." + name + ".";
			String type = required(props, prefix + "type");
			if (!"client".equals(type) && !"user".equals(type)) {
				throw new IllegalArgumentException(prefix + "type must be client or user, was " + type);
			}
			String clientId = required(props, prefix + "clientId");
			String username = "user".equals(type) ? required(props, prefix + "username") : null;
			List<String> roles = new ArrayList<>();
			for (String role : required(props, prefix + "roles").split(",")) {
				if (!role.isBlank()) {
					roles.add(role.trim());
				}
			}
			defs.add(new TokenDef(name, type, clientId, username, roles));
		}
		if (defs.isEmpty()) {
			throw new IllegalStateException("No token.<name>.* entries in " + PROPERTIES);
		}
		return defs;
	}

	/** Same claim layout as the Keycloak tokens the auth adapter validates offline. */
	private static String payload(TokenDef def, String issuer, Instant iat, Instant exp) {
		boolean user = "user".equals(def.type());
		String username = user ? def.username() : "service-account-" + def.clientId();
		String sub = user ? UUID.nameUUIDFromBytes(username.getBytes(StandardCharsets.UTF_8)).toString() : username;
		StringBuilder json = new StringBuilder("{");
		json.append("\"exp\":").append(exp.getEpochSecond());
		json.append(",\"iat\":").append(iat.getEpochSecond());
		json.append(",\"jti\":").append(str("local-iam-" + def.name() + "-" + iat.getEpochSecond()));
		json.append(",\"iss\":").append(str(issuer));
		json.append(",\"aud\":[").append(str("account")).append(',').append(str(def.clientId())).append(']');
		json.append(",\"sub\":").append(str(sub));
		json.append(",\"typ\":\"Bearer\"");
		json.append(",\"azp\":").append(str(def.clientId()));
		json.append(",\"realm_access\":{\"roles\":").append(strArray(def.roles())).append('}');
		json.append(",\"resource_access\":{").append(str(def.clientId())).append(":{\"roles\":[\"uma_protection\"]}}");
		json.append(",\"scope\":\"openid profile email\"");
		json.append(",\"preferred_username\":").append(str(username));
		if (user) {
			json.append(",\"email\":").append(str(username + "@local.mosip"));
		} else {
			json.append(",\"client_id\":").append(str(def.clientId()));
			json.append(",\"clientId\":").append(str(def.clientId()));
		}
		return json.append('}').toString();
	}

	private static String sign(RSAPrivateCrtKey key, String kid, String payload) throws Exception {
		String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":" + str(kid) + "}";
		String signingInput = b64(header.getBytes(StandardCharsets.UTF_8)) + "."
				+ b64(payload.getBytes(StandardCharsets.UTF_8));
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(key);
		signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
		return signingInput + "." + b64(signature.sign());
	}

	private static String mappings(List<Token> tokens, RSAPrivateCrtKey key, String kid, long expiresIn) {
		List<String> stubs = new ArrayList<>();
		stubs.add(stub("iam-jwks", 1, "GET", "/auth/realms/.*/protocol/openid-connect/certs", null,
				"\"headers\":{\"Content-Type\":\"application/json\"},\"jsonBody\":{\"keys\":[{\"kid\":" + str(kid)
						+ ",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":" + str(unsigned(key.getModulus()))
						+ ",\"e\":" + str(unsigned(key.getPublicExponent())) + "}]}"));
		for (Token token : tokens) {
			TokenDef def = token.def();
			String jwt = token.jwt();
			if ("client".equals(def.type())) {
				String match = "{\"contains\":" + str(def.clientId()) + "}";
				stubs.add(stub("iam-client-" + def.name(), 1, "POST", "/v1/authmanager/authenticate/clientidsecretkey",
						match, authmanagerResponse(jwt, "Clientid and Token combination had been validated successfully",
								expiresIn)));
				String oidcResponse = "\"headers\":{\"Content-Type\":\"application/json\"},\"jsonBody\":{\"access_token\":"
						+ str(jwt) + ",\"expires_in\":" + expiresIn
						+ ",\"token_type\":\"Bearer\",\"scope\":\"openid profile email\"}";
				stubs.add(stub("iam-oidc-token-" + def.name(), 1, "POST", "/auth/realms/.*/protocol/openid-connect/token",
						match, oidcResponse));
				// Clients such as the Keycloak admin client send client_id:secret as HTTP Basic auth instead.
				stubs.add(stub("iam-oidc-token-basic-" + def.name(), 2, "POST",
						"/auth/realms/.*/protocol/openid-connect/token", null,
						"{\"Authorization\":{\"matches\":" + str("Basic " + basicAuthPrefix(def.clientId()) + ".*") + "}}",
						oidcResponse));
			} else {
				String match = "{\"matchesJsonPath\":" + str("$.request[?(@.userName == '" + def.username() + "')]") + "}";
				String response = authmanagerResponse(jwt,
						"Username and password combination had been validated successfully", expiresIn);
				stubs.add(stub("iam-user-internal-" + def.name(), 1, "POST",
						"/v1/authmanager/authenticate/internal/useridPwd", match, response));
				stubs.add(stub("iam-user-" + def.name(), 1, "POST", "/v1/authmanager/authenticate/useridPwd", match,
						response));
			}
		}
		return "{\n  \"mappings\": [\n    " + String.join(",\n    ", stubs) + "\n  ]\n}\n";
	}

	private static String authmanagerResponse(String jwt, String message, long expiresIn) {
		return "\"headers\":{\"Content-Type\":\"application/json\",\"Authorization\":" + str(jwt)
				+ ",\"Set-Cookie\":" + str("Authorization=" + jwt + "; Path=/; HttpOnly")
				+ "},\"jsonBody\":{\"id\":\"string\",\"version\":\"v1\",\"responsetime\":\"2026-01-01T00:00:00.000Z\","
				+ "\"metadata\":null,\"response\":{\"status\":\"Success\",\"message\":" + str(message) + ",\"token\":"
				+ str(jwt) + ",\"expiryTime\":" + expiresIn + "},\"errors\":null}";
	}

	private static String stub(String name, int priority, String method, String urlPathPattern, String bodyPattern,
			String response) {
		return stub(name, priority, method, urlPathPattern, bodyPattern, null, response);
	}

	private static String stub(String name, int priority, String method, String urlPathPattern, String bodyPattern,
			String headerPatterns, String response) {
		return "{\"name\":" + str(name) + ",\"priority\":" + priority + ",\"request\":{\"method\":" + str(method)
				+ ",\"urlPathPattern\":" + str(urlPathPattern)
				+ (headerPatterns == null ? "" : ",\"headers\":" + headerPatterns)
				+ (bodyPattern == null ? "" : ",\"bodyPatterns\":[" + bodyPattern + "]") + "},\"response\":{\"status\":200,"
				+ response + "}}";
	}

	/**
	 * Regex for the start of base64("clientId:secret") that depends only on the client id: each full 3-byte group of
	 * "clientId:" encodes to 4 fixed characters, whatever the secret.
	 */
	private static String basicAuthPrefix(String clientId) {
		byte[] fixed = (clientId + ":").getBytes(StandardCharsets.UTF_8);
		byte[] whole = Arrays.copyOf(fixed, fixed.length - fixed.length % 3);
		return Pattern.quote(Base64.getEncoder().encodeToString(whole));
	}

	private static RSAPrivateCrtKey loadOrCreateKey(boolean newKey) throws Exception {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		if (newKey || !Files.exists(KEY_FILE)) {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			byte[] pkcs8 = generator.generateKeyPair().getPrivate().getEncoded();
			Files.createDirectories(KEY_FILE.getParent());
			Files.writeString(KEY_FILE, "-----BEGIN PRIVATE KEY-----\n"
					+ Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(pkcs8)
					+ "\n-----END PRIVATE KEY-----\n");
		}
		return (RSAPrivateCrtKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(readPem(KEY_FILE)));
	}

	private static byte[] readPem(Path file) throws IOException {
		String pem = Files.readString(file);
		if (!pem.contains("-----BEGIN PRIVATE KEY-----")) {
			throw new IllegalStateException(file + " must be an unencrypted PKCS#8 PEM (BEGIN PRIVATE KEY)");
		}
		String body = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
		return Base64.getDecoder().decode(body);
	}

	private static String required(Properties props, String key) {
		String value = props.getProperty(key);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing " + key + " in " + PROPERTIES);
		}
		return value.trim();
	}

	private static String unsigned(BigInteger value) {
		byte[] bytes = value.toByteArray();
		if (bytes.length > 1 && bytes[0] == 0) {
			byte[] trimmed = new byte[bytes.length - 1];
			System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
			bytes = trimmed;
		}
		return b64(bytes);
	}

	private static String b64(byte[] bytes) {
		return B64URL.encodeToString(bytes);
	}

	private static String strArray(List<String> values) {
		List<String> quoted = new ArrayList<>();
		for (String value : values) {
			quoted.add(str(value));
		}
		return "[" + String.join(",", quoted) + "]";
	}

	private static String str(String value) {
		StringBuilder out = new StringBuilder("\"");
		for (char c : value.toCharArray()) {
			switch (c) {
			case '"' -> out.append("\\\"");
			case '\\' -> out.append("\\\\");
			case '\n' -> out.append("\\n");
			case '\r' -> out.append("\\r");
			case '\t' -> out.append("\\t");
			default -> {
				if (c < 0x20) {
					out.append(String.format("\\u%04x", (int) c));
				} else {
					out.append(c);
				}
			}
			}
		}
		return out.append('"').toString();
	}
}
