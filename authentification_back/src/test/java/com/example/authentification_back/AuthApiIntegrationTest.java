package com.example.authentification_back;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration HTTP (MockMvc) pour le TP1 : profil {@code test} + H2 (voir {@code application-test.properties}).
 * Le compte {@code toto@example.com} est créé par {@link com.example.authentification_back.config.TestAccountInitializer}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void register_rejects_invalid_email_format() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"pas-un-email\",\"password\":\"1234\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_rejects_password_shorter_than_four() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"user@example.com\",\"password\":\"abc\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_ok() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"newuser@example.com\",\"password\":\"1234\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("newuser@example.com"));
	}

	@Test
	void register_conflict_when_email_exists() throws Exception {
		String body = "{\"email\":\"dup@example.com\",\"password\":\"1234\"}";
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void login_ok_with_test_account() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"toto@example.com\",\"password\":\"pwd1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("toto@example.com"))
				.andExpect(jsonPath("$.token").exists());
	}

	@Test
	void login_fails_when_password_wrong() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"toto@example.com\",\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void login_fails_when_email_unknown() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"nobody@example.com\",\"password\":\"pwd1234\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Email inconnu"));
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
						.content("{\"email\":\"toto@example.com\",\"password\":\"pwd1234\"}"))
				.andExpect(status().isOk())
				.andReturn();
		String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
		MvcResult me = mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("toto@example.com"))
				.andReturn();
		assertThat(objectMapper.readTree(me.getResponse().getContentAsString()).has("token")).isFalse();
	}
}
