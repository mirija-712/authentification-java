package com.example.authentification_back;

import com.example.authentification_back.config.TestAccountInitializer;
import com.example.authentification_back.security.SsoHmac;
import com.example.authentification_back.service.AuthService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration TP3 (MockMvc, H2, profil {@code test}). Pas de {@code @Transactional} sur la classe :
 * avec MockMvc, une transaction de test enveloppante ferait annuler les échecs de login (compteur / verrou)
 * avant la requête suivante.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

	private static final String STRONG = "Aa1!aaaaaaaa";

	@Autowired
	private MockMvc mockMvc;

	private static String registerJson(String email, String pass, String confirm) {
		return String.format(
				"{\"email\":\"%s\",\"password\":\"%s\",\"passwordConfirm\":\"%s\"}",
				email, pass, confirm);
	}

	private static String loginHmacJson(String email, String passwordPlain, String nonce, long epochSeconds) {
		String em = email.trim().toLowerCase(Locale.ROOT);
		String msg = SsoHmac.messageToSign(em, nonce, epochSeconds);
		String hmac = SsoHmac.hmacSha256Hex(passwordPlain, msg);
		return String.format(Locale.ROOT,
				"{\"email\":\"%s\",\"nonce\":\"%s\",\"timestamp\":%d,\"hmac\":\"%s\"}",
				em, nonce, epochSeconds, hmac);
	}

	private static String changePasswordJson(String oldPassword, String newPassword, String confirmPassword) {
		return String.format(Locale.ROOT,
				"{\"oldPassword\":\"%s\",\"newPassword\":\"%s\",\"confirmPassword\":\"%s\"}",
				oldPassword, newPassword, confirmPassword);
	}

	private String loginAndExtractToken(String email, String password) throws Exception {
		String nonce = UUID.randomUUID().toString();
		long ts = Instant.now().getEpochSecond();
		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson(email, password, nonce, ts)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.token");
	}

	@Test
	void register_rejects_invalid_email_format() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson("pas-un-email", STRONG, STRONG)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_rejects_weak_password() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson("user@example.com", "short", "short")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_rejects_password_confirm() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson("confirm@example.com", STRONG, "Bb2!bbbbbbbb")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Les mots de passe ne correspondent pas"));
	}

	@Test
	void register_ok() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson("newuser@example.com", STRONG, STRONG)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("newuser@example.com"));
	}

	@Test
	void register_conflict_when_email_exists() throws Exception {
		String body = registerJson("dup@example.com", STRONG, STRONG);
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void login_hmac_ok_with_test_account() throws Exception {
		String nonce = UUID.randomUUID().toString();
		long ts = Instant.now().getEpochSecond();
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson(
								TestAccountInitializer.TEST_EMAIL,
								TestAccountInitializer.TEST_PASSWORD_PLAIN,
								nonce,
								ts)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists());
	}

	@Test
	void login_rejects_invalid_email_format() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"bad\",\"nonce\":\"n\",\"timestamp\":1,\"hmac\":\"ab\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void login_fails_with_wrong_hmac() throws Exception {
		String nonce = UUID.randomUUID().toString();
		long ts = Instant.now().getEpochSecond();
		String em = TestAccountInitializer.TEST_EMAIL.trim().toLowerCase(Locale.ROOT);
		String msg = SsoHmac.messageToSign(em, nonce, ts);
		String badHmac = SsoHmac.hmacSha256Hex("wrong-password", msg);
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(Locale.ROOT,
								"{\"email\":\"%s\",\"nonce\":\"%s\",\"timestamp\":%d,\"hmac\":\"%s\"}",
								em, nonce, ts, badHmac)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));
	}

	@Test
	void login_fails_when_timestamp_too_old() throws Exception {
		String nonce = UUID.randomUUID().toString();
		long ts = Instant.now().getEpochSecond() - 120;
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson(
								TestAccountInitializer.TEST_EMAIL,
								TestAccountInitializer.TEST_PASSWORD_PLAIN,
								nonce,
								ts)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));
	}

	@Test
	void login_fails_when_timestamp_too_far_in_future() throws Exception {
		String nonce = UUID.randomUUID().toString();
		long ts = Instant.now().getEpochSecond() + 120;
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson(
								TestAccountInitializer.TEST_EMAIL,
								TestAccountInitializer.TEST_PASSWORD_PLAIN,
								nonce,
								ts)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));
	}

	@Test
	void login_fails_on_nonce_replay() throws Exception {
		String nonce = UUID.randomUUID().toString();
		long ts = Instant.now().getEpochSecond();
		String body = loginHmacJson(
				TestAccountInitializer.TEST_EMAIL,
				TestAccountInitializer.TEST_PASSWORD_PLAIN,
				nonce,
				ts);
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));
	}

	@Test
	void login_fails_for_unknown_email() throws Exception {
		String nonce = UUID.randomUUID().toString();
		long ts = Instant.now().getEpochSecond();
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson("nobody@example.com", "AnyPass1!", nonce, ts)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));
	}

	@Test
	void me_forbidden_without_token() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void me_ok_after_login_with_bearer_token() throws Exception {
		String nonce = UUID.randomUUID().toString();
		long ts = Instant.now().getEpochSecond();
		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson(
								TestAccountInitializer.TEST_EMAIL,
								TestAccountInitializer.TEST_PASSWORD_PLAIN,
								nonce,
								ts)))
				.andExpect(status().isOk())
				.andReturn();
		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");
		MvcResult me = mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(TestAccountInitializer.TEST_EMAIL))
				.andReturn();
		Map<String, Object> meBody = JsonPath.read(me.getResponse().getContentAsString(), "$");
		assertThat(meBody.containsKey("token")).isFalse();
	}

	@Test
	void account_locks_after_five_failures_then_unlocks_after_delay() throws Exception {
		String email = "lockout@example.com";
		String strong = "Bb2!bbbbbbbb";
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson(email, strong, strong)))
				.andExpect(status().isCreated());
		String emLower = email.trim().toLowerCase(Locale.ROOT);
		for (int i = 0; i < 5; i++) {
			String n = UUID.randomUUID().toString();
			long t = Instant.now().getEpochSecond();
			String badHmac = SsoHmac.hmacSha256Hex("nope", SsoHmac.messageToSign(emLower, n, t));
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(String.format(Locale.ROOT,
									"{\"email\":\"%s\",\"nonce\":\"%s\",\"timestamp\":%d,\"hmac\":\"%s\"}",
									emLower, n, t, badHmac)))
					.andExpect(status().isUnauthorized());
		}
		String nonce6 = UUID.randomUUID().toString();
		long ts6 = Instant.now().getEpochSecond();
		String badHmac6 = SsoHmac.hmacSha256Hex("nope", SsoHmac.messageToSign(emLower, nonce6, ts6));
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(Locale.ROOT,
								"{\"email\":\"%s\",\"nonce\":\"%s\",\"timestamp\":%d,\"hmac\":\"%s\"}",
								emLower, nonce6, ts6, badHmac6)))
				.andExpect(status().is(HttpStatus.LOCKED.value()));
		Thread.sleep(3100);
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson(email, strong, UUID.randomUUID().toString(), Instant.now().getEpochSecond())))
				.andExpect(status().isOk());
	}

	@Test
	void change_password_success_then_old_login_fails_and_new_login_works() throws Exception {
		String email = "tp5-success@example.com";
		String oldPassword = "Bb2!bbbbbbbb";
		String newPassword = "Cc3!cccccccc";
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson(email, oldPassword, oldPassword)))
				.andExpect(status().isCreated());
		String token = loginAndExtractToken(email, oldPassword);

		mockMvc.perform(put("/api/auth/change-password")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.content(changePasswordJson(oldPassword, newPassword, newPassword)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Mot de passe changé avec succès"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson(email, oldPassword, UUID.randomUUID().toString(), Instant.now().getEpochSecond())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginHmacJson(email, newPassword, UUID.randomUUID().toString(), Instant.now().getEpochSecond())))
				.andExpect(status().isOk());
	}

	@Test
	void change_password_rejects_when_old_password_is_wrong() throws Exception {
		String email = "tp5-wrong-old@example.com";
		String oldPassword = "Dd4!dddddddd";
		String newPassword = "Ee5!eeeeeeee";
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson(email, oldPassword, oldPassword)))
				.andExpect(status().isCreated());
		String token = loginAndExtractToken(email, oldPassword);

		mockMvc.perform(put("/api/auth/change-password")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.content(changePasswordJson("WrongPwd1!", newPassword, newPassword)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(AuthService.CHANGE_PASSWORD_OLD_PASSWORD_ERROR));
	}

	@Test
	void change_password_rejects_when_confirmation_differs() throws Exception {
		String token = loginAndExtractToken(
				TestAccountInitializer.TEST_EMAIL,
				TestAccountInitializer.TEST_PASSWORD_PLAIN);

		mockMvc.perform(put("/api/auth/change-password")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.content(changePasswordJson("Pwd1234!abcd", "Ff6!ffffffff", "Mismatch1!")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Les mots de passe ne correspondent pas"));
	}

	@Test
	void change_password_rejects_weak_new_password() throws Exception {
		String token = loginAndExtractToken(
				TestAccountInitializer.TEST_EMAIL,
				TestAccountInitializer.TEST_PASSWORD_PLAIN);

		mockMvc.perform(put("/api/auth/change-password")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.content(changePasswordJson("Pwd1234!abcd", "weak", "weak")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void change_password_rejects_invalid_user_or_token() throws Exception {
		mockMvc.perform(put("/api/auth/change-password")
						.contentType(MediaType.APPLICATION_JSON)
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
						.content(changePasswordJson("AnyOld1!", "Gg7!gggggggg", "Gg7!gggggggg")))
				.andExpect(status().isUnauthorized());
	}
}
