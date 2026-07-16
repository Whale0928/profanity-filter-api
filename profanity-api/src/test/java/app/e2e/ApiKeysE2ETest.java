package app.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import app.application.auth.LoginAuthService;
import app.core.data.response.constant.StatusCode;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

class ApiKeysE2ETest extends AbstractApiTester {
  @Autowired private MockMvc mockMvc;
  @Autowired private LoginAuthService loginAuthService;
  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("기존 API Key는 최초 SSO 로그인 시 검증된 이메일로 한 번만 연결된다")
  void login_matchingLegacyEmail_claimsMigratedKeyOnce() throws Exception {
    String token = login("e2e-read@example.com", "legacy-owner");

    awaitOwnership("e2e-read@example.com");
    JsonNode first = body(list(token)).at("/data");
    String secondToken = login("e2e-read@example.com", "legacy-owner");
    awaitOwnership("e2e-read@example.com");
    JsonNode second = body(list(secondToken)).at("/data");

    assertThat(first).hasSize(1);
    assertThat(first.get(0).at("/keyHint").asText()).isEqualTo("Hmikqf...-nDU");
    assertThat(second).hasSize(1);
  }

  @Test
  @DisplayName("로그인 사용자는 여러 API Key를 발급하고 목록에서는 원문을 다시 볼 수 없다")
  void issue_multipleKeys_returnsPlaintextOnlyFromIssueResponse() throws Exception {
    String token = login("keys-owner@gmail.com", "keys-owner");

    JsonNode first = body(issue(token, "개발", "dev-server")).at("/data");
    JsonNode second = body(issue(token, "운영", "prod-server")).at("/data");
    JsonNode keys = body(list(token)).at("/data");

    assertThat(first.at("/apiKey").asText()).isNotBlank();
    assertThat(second.at("/apiKey").asText()).isNotEqualTo(first.at("/apiKey").asText());
    assertThat(keys).hasSize(2);
    assertThat(keys.toString()).doesNotContain(first.at("/apiKey").asText());
    assertThat(keys.toString()).doesNotContain(second.at("/apiKey").asText());
    assertThat(keys.get(0).at("/email").asText()).isEqualTo("keys-owner@gmail.com");
  }

  @Test
  @DisplayName("재발행하면 이전 API Key 인증은 실패하고 새 API Key는 성공한다")
  void reissue_activeKey_rotatesAuthenticationCredential() throws Exception {
    String token = login("rotation-owner@gmail.com", "rotation-owner");
    JsonNode issued = body(issue(token, "운영", "prod-server")).at("/data");
    String oldKey = issued.at("/apiKey").asText();
    String keyId = issued.at("/key/id").asText();

    MockHttpServletResponse reissued =
        mockMvc
            .perform(
                post("/api/v1/dashboard/keys/{keyId}/reissue", keyId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andReturn()
            .getResponse();
    String newKey = body(reissued).at("/data/apiKey").asText();

    assertThat(filter(oldKey).at("/status/code").asInt())
        .isEqualTo(StatusCode.NOT_FOUND_CLIENT.code());
    assertThat(filter(newKey).at("/status/code").asInt()).isEqualTo(StatusCode.OK.code());
  }

  @Test
  @DisplayName("만료는 멱등 처리되고 만료된 API Key 인증을 거부한다")
  void expire_twice_isIdempotentAndRejectsCredential() throws Exception {
    String token = login("expire-owner@gmail.com", "expire-owner");
    JsonNode issued = body(issue(token, "일회성", "batch-server")).at("/data");
    String apiKey = issued.at("/apiKey").asText();
    String keyId = issued.at("/key/id").asText();

    JsonNode first = body(expire(token, keyId)).at("/data");
    JsonNode second = body(expire(token, keyId)).at("/data");

    assertThat(second.at("/expiredAt").asText()).isEqualTo(first.at("/expiredAt").asText());
    assertThat(filter(apiKey).at("/status/code").asInt())
        .isEqualTo(StatusCode.NOT_FOUND_CLIENT.code());
  }

  @Test
  @DisplayName("다른 사용자의 API Key는 조회하거나 변경할 수 없다")
  void ownership_otherUser_isIsolated() throws Exception {
    String ownerToken = login("owner@gmail.com", "owner");
    String otherToken = login("other@gmail.com", "other");
    String keyId = body(issue(ownerToken, "운영", "owner-server")).at("/data/key/id").asText();

    assertThat(body(list(otherToken)).at("/data")).isEmpty();
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/api/v1/dashboard/keys/{keyId}/reissue", keyId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
            .andReturn()
            .getResponse();

    assertThat(body(response).at("/status/code").asInt())
        .isEqualTo(StatusCode.API_KEY_NOT_FOUND.code());
  }

  private MockHttpServletResponse issue(String token, String name, String issuerInfo)
      throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/dashboard/keys")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("name", name, "issuerInfo", issuerInfo, "note", "test"))))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse list(String token) throws Exception {
    return mockMvc
        .perform(get("/api/v1/dashboard/keys").header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andReturn()
        .getResponse();
  }

  private MockHttpServletResponse expire(String token, String keyId) throws Exception {
    return mockMvc
        .perform(
            delete("/api/v1/dashboard/keys/{keyId}", keyId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andReturn()
        .getResponse();
  }

  private JsonNode filter(String apiKey) throws Exception {
    MockHttpServletResponse response =
        mockMvc
            .perform(
                post("/api/v1/filter")
                    .header("X-API-KEY", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"text\":\"hello\",\"mode\":\"QUICK\"}"))
            .andReturn()
            .getResponse();
    return body(response);
  }

  private String login(String email, String providerUserId) {
    String code =
        loginAuthService.issueExchangeCode(
            new OAuthLoginProfile(
                OAuthProvider.GOOGLE,
                providerUserId,
                email,
                true,
                true,
                email,
                providerUserId,
                null));
    return loginAuthService.exchange(code).accessToken();
  }

  private void awaitOwnership(String email) throws Exception {
    Instant deadline = Instant.now().plus(Duration.ofSeconds(3));
    while (Instant.now().isBefore(deadline)) {
      if (ownedKeyCount(email) == 1) return;
      Thread.sleep(25);
    }
    assertThat(ownedKeyCount(email)).isEqualTo(1);
  }

  private int ownedKeyCount(String email) throws Exception {
    try (var connection = dataSource.getConnection();
        var statement =
            connection.prepareStatement(
                "SELECT COUNT(*) FROM api_keys WHERE email = ? AND user_id IS NOT NULL")) {
      statement.setString(1, email);
      try (var result = statement.executeQuery()) {
        result.next();
        return result.getInt(1);
      }
    }
  }

  private JsonNode body(MockHttpServletResponse response) throws Exception {
    return objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }
}
