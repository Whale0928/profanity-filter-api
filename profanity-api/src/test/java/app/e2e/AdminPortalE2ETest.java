package app.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import app.application.auth.LoginAuthService;
import app.application.manage.SyncHandler;
import app.core.data.constant.Mode;
import app.core.data.response.constant.StatusCode;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import app.test.support.fixture.SeedApiKeys;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@TestPropertySource(
    properties = {
      "spring.datasource.hikari.maximum-pool-size=1",
      "spring.datasource.hikari.connection-timeout=1000"
    })
class AdminPortalE2ETest extends AbstractApiTester {
  @Autowired private MockMvc mockMvc;
  @Autowired private LoginAuthService loginAuthService;
  @Autowired private DataSource dataSource;
  @Autowired private SyncHandler syncHandler;

  @Test
  @DisplayName("수동 사전 동기화도 이전 필터 결과 캐시를 무효화한다")
  void manualSync_invalidatesFilterCache() throws Exception {
    assertThat(filter("수동동기화검증").path("detected")).isEmpty();
    try (var connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.executeUpdate("INSERT INTO profanity_word(word,is_used) VALUES ('수동동기화검증','Y')");
      statement.executeUpdate(
          "INSERT INTO manage_account(username,password) VALUES ('sync-test','test-only-sync-password')");
    }
    syncHandler.doSync("test-only-sync-password");
    assertThat(filter("수동동기화검증").path("detected")).hasSize(1);
  }

  @Test
  @DisplayName("API Key 인증과 최근 사용 기록은 DB 연결 하나로 처리된다")
  void apiKeyUsage_worksWithSingleDatabaseConnection() throws Exception {
    filter("사용기록검증");
    assertThat(count("SELECT COUNT(*) FROM api_keys WHERE last_used_at IS NOT NULL")).isEqualTo(1);
  }

  @Test
  @DisplayName("같은 단어 요청을 동시에 승인해도 사전과 감사 기록은 한 번만 저장된다")
  void wordApproval_concurrentRequests_applyOnce() throws Exception {
    Login owner = login("concurrent-owner", false);
    Login admin = login("concurrent-admin", true);
    String id =
        data(write(
                post("/api/v1/dashboard/inquiries"),
                owner.token(),
                Map.of(
                    "type",
                    "WORD_REQUEST",
                    "title",
                    "동시 승인 검증",
                    "content",
                    "추가 요청",
                    "word",
                    "동시승인검증표현",
                    "requestType",
                    "ADD",
                    "severity",
                    "LOW")))
            .path("id")
            .asText();
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> approve =
          () -> {
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return body(write(
                    post("/api/v1/admin/inquiries/" + id + "/word-decision"),
                    admin.token(),
                    Map.of("decision", "APPROVE", "reason", "검토 완료")))
                .at("/status/code")
                .asInt();
          };
      var first = executor.submit(approve);
      var second = executor.submit(approve);
      start.countDown();
      assertThat(
              java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(2000, 4084);
    }
    assertThat(count("SELECT COUNT(*) FROM profanity_word WHERE word='동시승인검증표현'")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM admin_audit_logs WHERE action='WORD_REQUEST_APPROVED'"))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("CLIENT와 API Key는 관리자 API에 접근할 수 없고 역할 변경은 기존 JWT에도 반영된다")
  void adminAccess_requiresCurrentDatabaseAdminRoleAndLoginCredential() throws Exception {
    Login client = login("role-client", false);
    assertThat(data(request(get("/api/v1/auth/me"), client.token())).path("role").asText())
        .isEqualTo("CLIENT");
    assertThat(request(get("/api/v1/admin/news"), client.token()).getStatus()).isEqualTo(403);
    assertThat(request(get("/api/v1/admin/news"), null).getStatus()).isEqualTo(401);
    var apiKeyAttempt =
        request(
            get("/api/v1/admin/news").header("X-API-KEY", SeedApiKeys.WRITE_CLIENT.apiKey()), null);
    assertThat(apiKeyAttempt.getStatus()).isIn(401, 403);
    setRole(client.id(), "ADMIN");
    assertThat(request(get("/api/v1/admin/news"), client.token()).getStatus()).isEqualTo(200);
    setRole(client.id(), "CLIENT");
    assertThat(request(get("/api/v1/admin/news"), client.token()).getStatus()).isEqualTo(403);
  }

  @Test
  @DisplayName("CLIENT는 모든 관리자 조회와 변경 경로에서 차단된다")
  void clientCannotAccessAnyAdminOperation() throws Exception {
    Login client = login("all-admin-paths-client", false);
    String uuid = "00000000-0000-0000-0000-000000000001";
    var operations =
        java.util.List.of(
            get("/api/v1/admin/news"),
            post("/api/v1/admin/news"),
            put("/api/v1/admin/news/1"),
            delete("/api/v1/admin/news/1"),
            get("/api/v1/admin/users"),
            patch("/api/v1/admin/users/" + uuid + "/status"),
            get("/api/v1/admin/keys"),
            post("/api/v1/admin/keys/" + uuid + "/revoke"),
            get("/api/v1/admin/words"),
            post("/api/v1/admin/words"),
            put("/api/v1/admin/words/1"),
            get("/api/v1/admin/inquiries"),
            get("/api/v1/admin/inquiries/1"),
            patch("/api/v1/admin/inquiries/1/status"),
            post("/api/v1/admin/inquiries/1/replies"),
            post("/api/v1/admin/inquiries/1/word-decision"));
    for (var operation : operations) {
      assertThat(
              request(
                      operation.contentType(MediaType.APPLICATION_JSON).content("{}"),
                      client.token())
                  .getStatus())
          .isEqualTo(403);
    }
    assertThat(count("SELECT COUNT(*) FROM admin_audit_logs")).isZero();
    assertThat(data(request(get("/api/v1/auth/me"), client.token())).path("role").asText())
        .isEqualTo("CLIENT");
  }

  @Test
  @DisplayName("임시 저장 소식은 공개되지 않고 게시와 비공개 전환이 실제 저장소에 반영된다")
  void news_publishAndUnpublish_preservesPublicBoundary() throws Exception {
    Login admin = login("news-admin", true);
    var draft =
        Map.of(
            "title",
            "배포 안내",
            "category",
            "CHANGELOG",
            "content",
            "## 변경 내용\n\n안내 본문",
            "status",
            "DRAFT");
    String id = data(write(post("/api/v1/admin/news"), admin.token(), draft)).path("id").asText();
    assertThat(id).isNotBlank();
    assertThat(request(get("/api/v1/news/" + id), null).getStatus()).isEqualTo(404);
    assertThat(data(request(get("/api/v1/news"), null)).path("items")).isEmpty();
    write(
        put("/api/v1/admin/news/" + id),
        admin.token(),
        Map.of(
            "title", "배포 안내", "category", "CHANGELOG", "content", "공개 본문", "status", "PUBLISHED"));
    assertThat(data(request(get("/api/v1/news/" + id), null)).path("content").asText())
        .isEqualTo("공개 본문");
    assertThat(data(request(get("/api/v1/news"), null)).path("items")).hasSize(1);
    write(put("/api/v1/admin/news/" + id), admin.token(), draft);
    assertThat(request(get("/api/v1/news/" + id), null).getStatus()).isEqualTo(404);
    assertThat(data(request(get("/api/v1/news"), null)).path("items")).isEmpty();
    assertThat(count("SELECT COUNT(*) FROM admin_audit_logs")).isEqualTo(3);
  }

  @Test
  @DisplayName("일반 문의는 본인만 조회하고 관리자 답변은 문의 완료와 함께 저장된다")
  void inquiry_replyAndOwnership_areEnforced() throws Exception {
    Login owner = login("inquiry-owner", false);
    Login other = login("inquiry-other", false);
    Login admin = login("inquiry-admin", true);
    String id =
        data(write(
                post("/api/v1/dashboard/inquiries"),
                owner.token(),
                Map.of("type", "GENERAL", "title", "연동 문의", "content", "연동 방법을 안내해 주세요.")))
            .path("id")
            .asText();
    var denied = request(get("/api/v1/dashboard/inquiries/" + id), other.token());
    assertThat(denied.getStatus()).isIn(403, 404);
    assertThat(data(request(get("/api/v1/dashboard/inquiries"), other.token())).path("items"))
        .isEmpty();
    write(
        post("/api/v1/admin/inquiries/" + id + "/replies"),
        admin.token(),
        Map.of("content", "대시보드를 확인해 주세요.", "resolve", true));
    JsonNode detail = data(request(get("/api/v1/dashboard/inquiries/" + id), owner.token()));
    assertThat(detail.path("status").asText()).isEqualTo("RESOLVED");
    assertThat(detail.path("replies")).hasSize(1);
    assertThat(detail.path("replies").get(0).path("content").asText()).isEqualTo("대시보드를 확인해 주세요.");
    assertThat(count("SELECT COUNT(*) FROM word_management")).isZero();
  }

  @Test
  @DisplayName("로그인 단어 요청은 API Key 없이 저장되며 답변 완료만으로 사전에 반영되지 않는다")
  void wordInquiry_withoutApiKey_requiresSeparateApproval() throws Exception {
    Login client = login("word-owner", false);
    Login admin = login("word-admin", true);
    String id =
        data(write(
                post("/api/v1/dashboard/inquiries"),
                client.token(),
                Map.of(
                    "type",
                    "WORD_REQUEST",
                    "title",
                    "표현 추가",
                    "content",
                    "검토 요청",
                    "word",
                    "검증전용표현",
                    "requestType",
                    "ADD",
                    "severity",
                    "LOW")))
            .path("id")
            .asText();
    assertThat(
            count(
                "SELECT COUNT(*) FROM word_management WHERE request_user_id IS NULL AND inquiry_id="
                    + Long.parseLong(id)))
        .isEqualTo(1);
    write(
        post("/api/v1/admin/inquiries/" + id + "/replies"),
        admin.token(),
        Map.of("content", "답변을 남깁니다.", "resolve", true));
    assertThat(count("SELECT COUNT(*) FROM profanity_word WHERE word='검증전용표현'")).isZero();
    write(
        post("/api/v1/admin/inquiries/" + id + "/word-decision"),
        admin.token(),
        Map.of("decision", "APPROVE", "reason", "사전 검토 완료"));
    assertThat(
            count(
                "SELECT COUNT(*) FROM profanity_word WHERE word='검증전용표현' AND is_used='Y' AND source='REQUEST'"))
        .isEqualTo(1);
    var duplicate =
        write(
            post("/api/v1/admin/inquiries/" + id + "/word-decision"),
            admin.token(),
            Map.of("decision", "APPROVE", "reason", "중복 요청"));
    assertThat(body(duplicate).path("status").path("code").asInt()).isNotEqualTo(2000);
    assertThat(count("SELECT COUNT(*) FROM profanity_word WHERE word='검증전용표현'")).isEqualTo(1);
  }

  @Test
  @DisplayName("외부 단어 요청은 기존 응답을 유지하고 API Key 관계로 문의를 함께 저장한다")
  void legacyWordRequest_createsLinkedInquiryWithoutChangingExternalContract() throws Exception {
    var response =
        write(
            post("/api/v1/word/request").header("X-API-KEY", SeedApiKeys.READ_CLIENT.apiKey()),
            null,
            Map.of("word", "연동검증표현", "reason", "검토 사유", "severity", "LOW", "type", "ADD"));
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(body(response).at("/status/code").asInt()).isEqualTo(2000);
    assertThat(
            count(
                "SELECT COUNT(*) FROM word_management w JOIN inquiries i ON w.inquiry_id=i.id WHERE w.request_user_id=i.requester_api_key_id AND w.word='연동검증표현' AND i.type='WORD_REQUEST'"))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("관리자 키 목록은 원문과 해시를 제외하고 만료와 계정 비활성화가 즉시 반영된다")
  void accountAndKeyManagement_revokeAccessAndNeverExposeKeyMaterial() throws Exception {
    Login admin = login("key-admin", true);
    Login client = login("key-owner", false);
    JsonNode created =
        data(
            write(
                post("/api/v1/dashboard/keys"),
                client.token(),
                Map.of("name", "검증용 키", "issuerInfo", "테스트", "note", "")));
    JsonNode list = data(request(get("/api/v1/admin/keys").param("query", "검증용 키"), admin.token()));
    assertThat(list.path("items")).hasSize(1);
    JsonNode key = list.path("items").get(0);
    assertThat(key.has("keyHash")).isFalse();
    assertThat(key.has("apiKey")).isFalse();
    assertThat(key.has("key")).isFalse();
    assertThat(created.isObject()).isTrue();
    String id = key.path("id").asText();
    data(
        write(
            post("/api/v1/admin/keys/" + id + "/revoke"),
            admin.token(),
            Map.of("reason", "사용 종료")));
    assertThat(
            count(
                "SELECT COUNT(*) FROM api_keys WHERE id=UNHEX(REPLACE('"
                    + id
                    + "','-','')) AND expired_at IS NOT NULL AND revoked_by IS NOT NULL"))
        .isEqualTo(1);
    data(
        write(
            patch("/api/v1/admin/users/" + client.id() + "/status"),
            admin.token(),
            Map.of("status", "DISABLED")));
    assertThat(request(get("/api/v1/auth/me"), client.token()).getStatus()).isIn(401, 403);
    var self =
        write(
            patch("/api/v1/admin/users/" + admin.id() + "/status"),
            admin.token(),
            Map.of("status", "DISABLED"));
    assertThat(body(self).at("/status/code").asInt()).isNotEqualTo(2000);
    assertThat(request(get("/api/v1/admin/users"), admin.token()).getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("사전 사용 상태 변경은 단어 수가 같아도 필터와 캐시에 반영된다")
  void dictionaryUsageChange_refreshesTrieAndCachedResponses() throws Exception {
    Login admin = login("dictionary-admin", true);
    String id =
        data(write(post("/api/v1/admin/words"), admin.token(), Map.of("word", "동기화검증표현")))
            .path("id")
            .asText();
    assertThat(filter("동기화검증표현").path("detected")).hasSize(1);
    data(
        write(
            put("/api/v1/admin/words/" + id),
            admin.token(),
            Map.of("word", "동기화검증표현", "isUsed", "N")));
    assertThat(filter("동기화검증표현").path("detected")).isEmpty();
    data(
        write(
            put("/api/v1/admin/words/" + id),
            admin.token(),
            Map.of("word", "새동기화검증표현", "isUsed", "Y")));
    assertThat(filter("동기화검증표현").path("detected")).isEmpty();
    assertThat(filter("새동기화검증표현").path("detected")).hasSize(1);
  }

  private JsonNode filter(String text) throws Exception {
    var response =
        write(
            post("/api/v1/filter").header("X-API-KEY", SeedApiKeys.READ_CLIENT.apiKey()),
            null,
            Map.of("text", text, "mode", "NORMAL"));
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(body(response).at("/status/code").asInt()).isEqualTo(2000);
    return body(response);
  }

  private Login login(String name, boolean admin) throws Exception {
    String code =
        loginAuthService.issueExchangeCode(
            new OAuthLoginProfile(
                OAuthProvider.GOOGLE,
                name,
                name + "@example.test",
                true,
                true,
                name + "@example.test",
                name,
                null));
    JsonNode token = data(write(post("/api/v1/auth/exchange"), null, Map.of("code", code)));
    String id = token.at("/user/id").asText();
    if (admin) setRole(id, "ADMIN");
    return new Login(id, token.path("accessToken").asText());
  }

  @Test
  @DisplayName("통계 개요는 기간 내 기록을 집계하고 허용하지 않는 기간은 거절한다")
  void statistics_aggregateRecordsAndRejectUnsupportedPeriod() throws Exception {
    Login admin = login("statistics-admin", true);

    JsonNode before = data(request(get("/api/v1/admin/statistics"), admin.token()));
    long totalBefore = before.at("/summary/totalRequests").asLong();
    long detectedBefore = before.at("/summary/detectedRequests").asLong();

    insertRecord("통계집계검증1", Mode.NORMAL, "바보");
    insertRecord("통계집계검증2", Mode.FILTER, "");
    insertRecord("통계집계검증3", Mode.NORMAL, "멍청이");

    JsonNode after = data(request(get("/api/v1/admin/statistics"), admin.token()));

    assertThat(after.at("/summary/totalRequests").asLong() - totalBefore).isEqualTo(3);
    assertThat(after.at("/summary/detectedRequests").asLong() - detectedBefore).isEqualTo(2);
    assertThat(after.at("/period/days").asInt()).isEqualTo(7);
    assertThat(after.path("daily")).hasSize(7);
    assertThat(after.path("modes")).hasSize(3);
    // 기록에 남긴 해시는 시드 API Key의 것이라 키 이름까지 이어 붙는다.
    assertThat(after.path("topApiKeys")).isNotEmpty();
    assertThat(after.at("/topApiKeys/0/name").asText()).isNotBlank();

    JsonNode rejected =
        body(request(get("/api/v1/admin/statistics").param("days", "14"), admin.token()));
    assertThat(rejected.at("/status/code").asInt()).isEqualTo(StatusCode.BAD_REQUEST.code());
  }

  /**
   * 기록을 직접 넣습니다. created_at은 서비스가 조회 경계를 만들 때와 같은 JVM 기본 시간대의 벽시계여야 하므로 SQL의 NOW()가 아니라 자바에서 계산한 값을
   * 넘깁니다.
   */
  private void insertRecord(String requestText, Mode mode, String words) throws Exception {
    try (var connection = dataSource.getConnection();
        var statement =
            connection.prepareStatement(
                """
                INSERT INTO records(tracking_id, api_key_hash, request_text, mode, words, created_at)
                VALUES (UNHEX(REPLACE(UUID(),'-','')), SHA2(?,256), ?, ?, ?, ?)
                """)) {
      statement.setString(1, SeedApiKeys.READ_CLIENT.apiKey());
      statement.setString(2, requestText);
      statement.setString(3, mode.name());
      statement.setString(4, words);
      statement.setObject(5, LocalDateTime.now());
      assertThat(statement.executeUpdate()).isEqualTo(1);
    }
  }

  private void setRole(String id, String role) throws Exception {
    try (var connection = dataSource.getConnection();
        var statement =
            connection.prepareStatement(
                "UPDATE users SET role=? WHERE id=UNHEX(REPLACE(?,'-',''))")) {
      statement.setString(1, role);
      statement.setString(2, id);
      assertThat(statement.executeUpdate()).isEqualTo(1);
    }
  }

  private long count(String sql) throws Exception {
    try (var connection = dataSource.getConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      result.next();
      return result.getLong(1);
    }
  }

  private MockHttpServletResponse request(MockHttpServletRequestBuilder request, String token)
      throws Exception {
    if (token != null) request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    return mockMvc.perform(request).andReturn().getResponse();
  }

  private MockHttpServletResponse write(
      MockHttpServletRequestBuilder request, String token, Object content) throws Exception {
    return request(
        request
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(content)),
        token);
  }

  private JsonNode body(MockHttpServletResponse response) throws Exception {
    return objectMapper.readTree(response.getContentAsString());
  }

  private JsonNode data(MockHttpServletResponse response) throws Exception {
    assertThat(response.getStatus()).isEqualTo(200);
    JsonNode body = body(response);
    assertThat(body.at("/status/code").asInt()).isEqualTo(2000);
    assertThat(body.path("data").isNull()).isFalse();
    return body.path("data");
  }

  private record Login(String id, String token) {}
}
