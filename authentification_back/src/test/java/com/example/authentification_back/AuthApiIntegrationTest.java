package com.example.authentification_back;

import com.example.authentification_back.config.TestAccountInitializer;
import com.example.authentification_back.security.Tp3Proof;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration TP2/TP3 (MockMvc, H2, profil {@code test}). Pas de {@code @Transactional} sur la classe :
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
	void challenge_unknown_email_returns_401() throws Exception {
		mockMvc.perform(post("/api/auth/challenge")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"inexistant@example.com\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void login_tp3_ok_with_test_account() throws Exception {
		MvcResult ch = mockMvc.perform(post("/api/auth/challenge")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format("{\"email\":\"%s\"}", TestAccountInitializer.TEST_EMAIL)))
				.andExpect(status().isOk())
				.andReturn();
		String nonce = JsonPath.read(ch.getResponse().getContentAsString(), "$.nonce");
		String authSalt = JsonPath.read(ch.getResponse().getContentAsString(), "$.authSalt");
		String fp = Tp3Proof.identityFingerprintHex(
				TestAccountInitializer.TEST_EMAIL, TestAccountInitializer.TEST_PASSWORD_PLAIN, authSalt);
		String proof = Tp3Proof.proofHex(fp, nonce);
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"nonce\":\"%s\",\"proof\":\"%s\"}",
								TestAccountInitializer.TEST_EMAIL, nonce, proof)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists());
	}

	@Test
	void login_ok_with_test_account() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"password\":\"%s\"}",
								TestAccountInitializer.TEST_EMAIL,
								TestAccountInitializer.TEST_PASSWORD_PLAIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(TestAccountInitializer.TEST_EMAIL))
				.andExpect(jsonPath("$.token").exists());
	}

	@Test
	void login_fails_with_same_generic_message_for_wrong_password() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"password\":\"wrong\"}",
								TestAccountInitializer.TEST_EMAIL)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));
	}

	@Test
	void login_fails_with_same_generic_message_for_unknown_email() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"password\":\"%s\"}",
								"nobody@example.com",
								TestAccountInitializer.TEST_PASSWORD_PLAIN)))
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
		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"password\":\"%s\"}",
								TestAccountInitializer.TEST_EMAIL,
								TestAccountInitializer.TEST_PASSWORD_PLAIN)))
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
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(String.format("{\"email\":\"%s\",\"password\":\"nope\"}", email)))
					.andExpect(status().isUnauthorized());
		}
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format("{\"email\":\"%s\",\"password\":\"nope\"}", email)))
				.andExpect(status().is(HttpStatus.LOCKED.value()));
		Thread.sleep(3100);
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, strong)))
				.andExpect(status().isOk());
	}
}
