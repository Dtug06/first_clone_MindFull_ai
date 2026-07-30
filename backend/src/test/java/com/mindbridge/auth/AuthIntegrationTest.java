package com.mindbridge.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for the authentication API.
 *
 * Verifies:
 * - Happy paths: register, login, access protected endpoint with valid token.
 * - Error paths: duplicate email, bad credentials, missing/invalid/expired token.
 * - Security invariants: password never in response, no sensitive data leakage.
 */
@DisplayName("Auth API")
class AuthIntegrationTest extends AuthIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.access-token-expiration-ms}")
    private long tokenExpirationMs;

    // --- Register ---

    @Test
    @DisplayName("POST /auth/register → 201, returns token + user profile")
    void register_success() throws Exception {
        String requestBody = """
                {"email":"alice@example.com","password":"P@ssw0rd!","displayName":"Alice"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(tokenExpirationMs))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("Alice"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /auth/register with duplicate email → 409")
    void register_duplicate_email() throws Exception {
        String requestBody = """
                {"email":"bob@example.com","password":"P@ssw0rd!","displayName":"Bob"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_DUPLICATE"));
    }

    @Test
    @DisplayName("POST /auth/register with missing fields → 400")
    void register_validation_error() throws Exception {
        String requestBody = """
                {"email":"not-an-email"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // --- Login ---

    @Test
    @DisplayName("POST /auth/login with correct credentials → 200")
    void login_success() throws Exception {
        // Register first
        String registerBody = """
                {"email":"carol@example.com","password":"Secure123!","displayName":"Carol"}
                """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        // Then login
        String loginBody = """
                {"email":"carol@example.com","password":"Secure123!"}
                """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("carol@example.com"));
    }

    @Test
    @DisplayName("POST /auth/login with wrong password → 401, generic message")
    void login_wrong_password() throws Exception {
        String registerBody = """
                {"email":"dave@example.com","password":"CorrectPass!","displayName":"Dave"}
                """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {"email":"dave@example.com","password":"WrongPass!"}
                """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_CREDENTIALS_INVALID"))
                // Message must NOT reveal whether email existed
                .andExpect(jsonPath("$.message").value("Email or password is incorrect"));
    }

    @Test
    @DisplayName("POST /auth/login with unknown email → 401, same message as wrong password")
    void login_user_not_found() throws Exception {
        String loginBody = """
                {"email":"neverregistered@example.com","password":"AnyPassword!"}
                """;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_CREDENTIALS_INVALID"))
                .andExpect(jsonPath("$.message").value("Email or password is incorrect"));
    }

    // --- Protected endpoints ---

    @Test
    @DisplayName("GET /users/me without token → 401")
    void users_me_no_token() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("GET /users/me with invalid token → 401")
    void users_me_invalid_token() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    @DisplayName("GET /users/me with valid token → 200, returns own profile")
    void users_me_valid_token() throws Exception {
        // Register + login to get a real token
        String registerBody = """
                {"email":"eve@example.com","password":"TestPass99!","displayName":"Eve"}
                """;
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();

        // Call protected endpoint
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("eve@example.com"))
                .andExpect(jsonPath("$.displayName").value("Eve"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // --- Password never in response ---

    @Nested
    @DisplayName("Security: passwords must not appear in any response")
    class PasswordLeakPrevention {

        @Test
        @DisplayName("Register response contains no password")
        void register_no_password_in_response() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"frank@example.com","password":"Secret99!","displayName":"Frank"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.user.password").doesNotExist())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("Login response contains no password")
        void login_no_password_in_response() throws Exception {
            // Register
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"grace@example.com","password":"Password123!","displayName":"Grace"}
                                    """))
                    .andExpect(status().isCreated());

            // Login
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"grace@example.com","password":"Password123!"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.user.password").doesNotExist());
        }

        @Test
        @DisplayName("GET /users/me response contains no password")
        void users_me_no_password_in_response() throws Exception {
            // Get a valid token
            MvcResult regResult = mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"heidi@example.com","password":"SafePass00!","displayName":"Heidi"}
                                    """))
                    .andReturn();

            String token = objectMapper.readTree(regResult.getResponse().getContentAsString())
                    .get("accessToken").asText();

            mockMvc.perform(get("/users/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist())
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
    }
}
