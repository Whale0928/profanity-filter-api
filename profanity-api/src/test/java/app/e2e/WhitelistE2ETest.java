package app.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import app.application.auth.LoginAuthService;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import app.test.support.fixture.SeedApiKeys;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 허용 단어 그룹(ADR 0009)을 여러 계정과 API Key로 검증합니다.
 *
 * <p>시드 사전에는 나쁜말샘플과 비속어샘플이 사용 중으로 들어 있고, 시드 API Key는 소유 계정이 연결되지 않은 과거 키입니다.
 */
class WhitelistE2ETest extends AbstractApiTester {

  private static final String BOTH = "나쁜말샘플 그리고 비속어샘플";
  private static final int OK = 2000;
  private static final int BAD_REQUEST = 4000;
  private static final int NOT_FOUND = 4090;
  private static final int LIMIT_EXCEEDED = 4091;
  private static final int OWNER_REQUIRED = 4092;

  @Autowired private MockMvc mockMvc;
  @Autowired private LoginAuthService loginAuthService;
  @Autowired private DataSource dataSource;

  @Nested
  @DisplayName("그룹 관리는")
  class Manage {

    @Test
    @DisplayName("만들고 조회하고 고치고 지우는 흐름이 계정 안에서 이어진다")
    void lifecycle() throws Exception {
      Login owner = login("lifecycle-owner");

      JsonNode created = data(createGroup(owner, "게임 욕설 허용 그룹", List.of("죽여", "처치")));
      String id = created.path("id").asText();
      assertThat(UUID.fromString(id)).isNotNull();
      assertThat(created.path("wordCount").asInt()).isEqualTo(2);
      assertThat(created.path("createdAt").asText()).isNotBlank();

      JsonNode listed = data(request(get("/api/v1/dashboard/whitelists"), owner.token()));
      assertThat(listed).hasSize(1);
      assertThat(listed.get(0).path("name").asText()).isEqualTo("게임 욕설 허용 그룹");

      JsonNode updated =
          data(
              write(
                  put("/api/v1/dashboard/whitelists/" + id),
                  owner.token(),
                  body("친구창 욕설 허용 그룹", List.of("바보"))));
      assertThat(updated.path("id").asText()).isEqualTo(id);
      assertThat(updated.path("words")).extracting(JsonNode::asText).containsExactly("바보");

      data(request(delete("/api/v1/dashboard/whitelists/" + id), owner.token()));
      assertThat(data(request(get("/api/v1/dashboard/whitelists"), owner.token()))).isEmpty();
    }

    @Test
    @DisplayName("단어를 정리해서 저장한다: 공백 제거, 빈 값 제외, 같은 단어 한 번만")
    void cleansWords() throws Exception {
      Login owner = login("clean-owner");

      JsonNode created =
          data(createGroup(owner, "  정리  ", List.of("  죽여 ", "", "죽여", "죽-여", "처치")));

      assertThat(created.path("name").asText()).isEqualTo("정리");
      assertThat(created.path("words")).extracting(JsonNode::asText).containsExactly("죽여", "처치");
    }

    @Test
    @DisplayName("규칙에 맞지 않는 이름과 단어는 이유와 함께 거절한다")
    void rejectsInvalidInput() throws Exception {
      Login owner = login("invalid-owner");
      List<String> tooMany =
          IntStream.range(0, 201).mapToObj(i -> "단어" + (char) ('가' + i)).toList();

      assertThat(code(createGroup(owner, "", List.of()))).isEqualTo(BAD_REQUEST);
      assertThat(code(createGroup(owner, "   ", List.of()))).isEqualTo(BAD_REQUEST);
      assertThat(code(createGroup(owner, "가".repeat(61), List.of()))).isEqualTo(BAD_REQUEST);
      assertThat(code(createGroup(owner, "그룹", List.of("가".repeat(81))))).isEqualTo(BAD_REQUEST);
      assertThat(code(createGroup(owner, "그룹", List.of("1234")))).isEqualTo(BAD_REQUEST);
      assertThat(code(createGroup(owner, "그룹", List.of("!!!")))).isEqualTo(BAD_REQUEST);
      assertThat(code(createGroup(owner, "그룹", tooMany))).isEqualTo(LIMIT_EXCEEDED);
      assertThat(count("SELECT COUNT(*) FROM whitelists")).isZero();
    }

    @Test
    @DisplayName("계정당 10개까지 만들 수 있고 11번째는 거절한다")
    void limitsGroupsPerAccount() throws Exception {
      Login owner = login("limit-owner");
      for (int i = 0; i < 10; i++) {
        assertThat(code(createGroup(owner, "그룹 " + i, List.of()))).isEqualTo(OK);
      }

      assertThat(code(createGroup(owner, "열한 번째", List.of()))).isEqualTo(LIMIT_EXCEEDED);
      assertThat(code(createGroup(login("other-owner"), "남의 첫 그룹", List.of()))).isEqualTo(OK);
    }

    @Test
    @DisplayName("동시에 만들어도 계정당 상한을 넘지 않는다")
    void concurrentCreatesRespectLimit() throws Exception {
      Login owner = login("concurrent-owner");
      int attempts = 16;
      var pool = Executors.newFixedThreadPool(attempts);
      var start = new CountDownLatch(1);
      var succeeded = new AtomicInteger();
      var failedUnexpectedly = new AtomicInteger();
      try {
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
          int index = i;
          futures.add(
              pool.submit(
                  () -> {
                    start.await();
                    int result = code(createGroup(owner, "동시 그룹 " + index, List.of()));
                    if (result == OK) succeeded.incrementAndGet();
                    else if (result != LIMIT_EXCEEDED) failedUnexpectedly.incrementAndGet();
                    return null;
                  }));
        }
        start.countDown();
        for (var future : futures) future.get(30, TimeUnit.SECONDS);
      } finally {
        pool.shutdownNow();
      }

      assertThat(failedUnexpectedly.get()).isZero();
      assertThat(succeeded.get()).isEqualTo(10);
      assertThat(count("SELECT COUNT(*) FROM whitelists")).isEqualTo(10);
    }

    @Test
    @DisplayName("남의 그룹은 보이지 않고 고치거나 지울 수 없다. 관리자여도 마찬가지다")
    void isolatesAccounts() throws Exception {
      Login owner = login("iso-owner");
      Login stranger = login("iso-stranger");
      Login admin = login("iso-admin");
      setRole(admin.id(), "ADMIN");
      String id = data(createGroup(owner, "내 그룹", List.of("죽여"))).path("id").asText();

      for (Login other : List.of(stranger, admin)) {
        assertThat(data(request(get("/api/v1/dashboard/whitelists"), other.token()))).isEmpty();
        assertThat(
                code(
                    write(
                        put("/api/v1/dashboard/whitelists/" + id),
                        other.token(),
                        body("탈취", List.of()))))
            .isEqualTo(NOT_FOUND);
        assertThat(code(request(delete("/api/v1/dashboard/whitelists/" + id), other.token())))
            .isEqualTo(NOT_FOUND);
      }
      // 없는 그룹도 같은 코드라서 남의 그룹이 존재하는지 구분할 수 없다.
      assertThat(
              code(
                  request(
                      delete("/api/v1/dashboard/whitelists/" + UUID.randomUUID()), owner.token())))
          .isEqualTo(NOT_FOUND);
      assertThat(
              data(request(get("/api/v1/dashboard/whitelists"), owner.token()))
                  .get(0)
                  .path("name")
                  .asText())
          .isEqualTo("내 그룹");
    }

    @Test
    @DisplayName("로그인하지 않았거나 API Key로는 그룹을 관리할 수 없다")
    void requiresLoginJwt() throws Exception {
      assertThat(request(get("/api/v1/dashboard/whitelists"), null).getStatus()).isEqualTo(401);
      assertThat(
              mockMvc
                  .perform(
                      get("/api/v1/dashboard/whitelists")
                          .header("X-API-KEY", SeedApiKeys.READ_CLIENT.apiKey()))
                  .andReturn()
                  .getResponse()
                  .getStatus())
          .isIn(401, 403);
      assertThat(request(get("/api/v1/dashboard/whitelists"), "not-a-jwt").getStatus())
          .isIn(401, 403);
    }

    @Test
    @DisplayName("경로의 그룹 ID 형식이 틀려도 서버 오류로 번지지 않는다")
    void malformedPathId() throws Exception {
      Login owner = login("malformed-owner");

      MockHttpServletResponse response =
          request(delete("/api/v1/dashboard/whitelists/not-a-uuid"), owner.token());

      assertThat(response.getStatus()).isLessThan(500);
      assertThat(code(response)).isNotEqualTo(OK);
    }
  }

  @Nested
  @DisplayName("필터 요청에 그룹을 지정하면")
  class Apply {

    @Test
    @DisplayName("지정하지 않은 요청은 기존과 똑같이 검출한다")
    void withoutIds_behavesAsBefore() throws Exception {
      Account account = account("plain");

      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", null)))
          .containsExactlyInAnyOrder("나쁜말샘플", "비속어샘플");
      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of())))
          .containsExactlyInAnyOrder("나쁜말샘플", "비속어샘플");
    }

    @Test
    @DisplayName("NORMAL은 허용 단어를 뺀 나머지만 검출한다")
    void normal() throws Exception {
      Account account = account("normal");
      String group = group(account, "게임", "나쁜말샘플");

      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(group))))
          .containsExactly("비속어샘플");
    }

    @Test
    @DisplayName("FILTER는 허용 단어를 그대로 두고 나머지만 가린다")
    void filterMode() throws Exception {
      Account account = account("mask");
      String group = group(account, "게임", "나쁜말샘플");

      JsonNode response = filter(account.apiKey(), BOTH, "FILTER", List.of(group));

      assertThat(detected(response)).containsExactly("비속어샘플");
      assertThat(response.path("filtered").asText()).isEqualTo("나쁜말샘플 그리고 *****");
    }

    @Test
    @DisplayName("QUICK은 첫 검출이 허용 단어이면 그 뒤의 검출을 돌려준다")
    void quick() throws Exception {
      Account account = account("quick");
      String group = group(account, "게임", "나쁜말샘플");

      assertThat(detected(filter(account.apiKey(), BOTH, "QUICK", null))).containsExactly("나쁜말샘플");
      assertThat(detected(filter(account.apiKey(), BOTH, "QUICK", List.of(group))))
          .containsExactly("비속어샘플");
    }

    @Test
    @DisplayName("여러 그룹을 지정하면 허용 단어를 합쳐서 적용한다")
    void union() throws Exception {
      Account account = account("union");
      String game = group(account, "게임 욕설 허용 그룹", "나쁜말샘플");
      String friends = group(account, "친구창 욕설 허용 그룹", "비속어샘플");

      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(game))))
          .containsExactly("비속어샘플");
      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(game, friends))))
          .isEmpty();
      assertThat(
              filter(account.apiKey(), BOTH, "FILTER", List.of(game, friends))
                  .path("filtered")
                  .asText())
          .isEqualTo(BOTH);
      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(game, game, game))))
          .containsExactly("비속어샘플");
    }

    @Test
    @DisplayName("원문에 기호가 끼어 있거나 허용 단어를 기호와 함께 등록해도 맞춰서 허용한다")
    void symbols() throws Exception {
      Account account = account("symbols");
      String group = group(account, "기호", "나쁜-말-샘플");

      assertThat(detected(filter(account.apiKey(), "나쁜말!샘플 입니다", "NORMAL", null))).hasSize(1);
      assertThat(detected(filter(account.apiKey(), "나쁜말!샘플 입니다", "NORMAL", List.of(group))))
          .isEmpty();
    }

    @Test
    @DisplayName("빈 그룹이나 사전에 없는 단어만 든 그룹은 결과를 바꾸지 않는다")
    void harmlessGroups() throws Exception {
      Account account = account("harmless");
      String empty = group(account, "빈 그룹");
      String unrelated = group(account, "무관", "헤드샷", "처치");

      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(empty, unrelated))))
          .containsExactlyInAnyOrder("나쁜말샘플", "비속어샘플");
    }

    @Test
    @DisplayName("form 요청도 같은 이름을 반복해서 여러 그룹을 지정한다")
    void formRequest() throws Exception {
      Account account = account("form");
      String game = group(account, "게임", "나쁜말샘플");
      String friends = group(account, "친구창", "비속어샘플");

      MockHttpServletResponse one =
          mockMvc
              .perform(
                  post("/api/v1/filter")
                      .header("X-API-KEY", account.apiKey())
                      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                      .param("text", BOTH)
                      .param("mode", "NORMAL")
                      .param("whitelistIds", game))
              .andReturn()
              .getResponse();
      MockHttpServletResponse two =
          mockMvc
              .perform(
                  post("/api/v1/filter")
                      .header("X-API-KEY", account.apiKey())
                      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                      .param("text", BOTH)
                      .param("mode", "NORMAL")
                      .param("whitelistIds", game, friends))
              .andReturn()
              .getResponse();

      assertThat(detected(json(one))).containsExactly("비속어샘플");
      assertThat(detected(json(two))).isEmpty();
    }

    @Test
    @DisplayName("기록에는 허용 단어를 적용한 뒤의 검출만 남는다")
    void records() throws Exception {
      Account account = account("records");
      String group = group(account, "게임", "나쁜말샘플");

      filter(account.apiKey(), "기록검증 " + BOTH, "NORMAL", List.of(group));

      assertThat(rows("SELECT words FROM records WHERE request_text LIKE '기록검증%'"))
          .containsExactly("비속어샘플");
    }
  }

  @Nested
  @DisplayName("잘못된 그룹 지정은")
  class Reject {

    @Test
    @DisplayName("없는 그룹, 남의 그룹, 지운 그룹을 모두 같은 코드로 거절한다")
    void missingForeignDeleted() throws Exception {
      Account mine = account("reject-mine");
      Account other = account("reject-other");
      String foreign = group(other, "남의 그룹", "나쁜말샘플");
      String deleted = group(mine, "지울 그룹", "나쁜말샘플");
      data(request(delete("/api/v1/dashboard/whitelists/" + deleted), mine.login().token()));

      for (String id : List.of(UUID.randomUUID().toString(), foreign, deleted)) {
        assertThat(code(filter(mine.apiKey(), BOTH, "NORMAL", List.of(id))))
            .as(id)
            .isEqualTo(NOT_FOUND);
      }
    }

    @Test
    @DisplayName("내 그룹에 남의 그룹이 하나 섞여도 전체를 거절하고 필터를 실행하지 않는다")
    void mixedIds() throws Exception {
      Account mine = account("mixed-mine");
      Account other = account("mixed-other");
      String valid = group(mine, "내 그룹", "나쁜말샘플");
      String foreign = group(other, "남의 그룹", "비속어샘플");

      JsonNode response = filter(mine.apiKey(), "섞임검증 " + BOTH, "NORMAL", List.of(valid, foreign));

      assertThat(code(response)).isEqualTo(NOT_FOUND);
      assertThat(count("SELECT COUNT(*) FROM records WHERE request_text LIKE '섞임검증%'")).isZero();
    }

    @Test
    @DisplayName("요청 한 번에 그룹을 5개까지만 받는다")
    void tooManyGroups() throws Exception {
      Account account = account("many");
      List<String> ids = new ArrayList<>();
      for (int i = 0; i < 6; i++) ids.add(group(account, "그룹 " + i));

      assertThat(code(filter(account.apiKey(), BOTH, "NORMAL", ids.subList(0, 5)))).isEqualTo(OK);
      assertThat(code(filter(account.apiKey(), BOTH, "NORMAL", ids)))
          .isIn(BAD_REQUEST, LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("ID 형식이 틀리면 서버 오류가 아니라 요청 오류로 응답한다")
    void malformedId() throws Exception {
      Account account = account("malformed");

      MockHttpServletResponse response =
          write(
              post("/api/v1/filter").header("X-API-KEY", account.apiKey()),
              null,
              Map.of("text", BOTH, "mode", "NORMAL", "whitelistIds", List.of("not-a-uuid")));

      assertThat(response.getStatus()).isLessThan(500);
      assertThat(code(response)).isNotEqualTo(OK);
    }

    @Test
    @DisplayName("소유 계정이 없는 API Key는 그룹을 쓸 수 없지만 그룹 없이는 그대로 동작한다")
    void keyWithoutOwner() throws Exception {
      Account account = account("ownerless-helper");
      String group = group(account, "게임", "나쁜말샘플");
      String legacyKey = SeedApiKeys.READ_CLIENT.apiKey();

      assertThat(code(filter(legacyKey, BOTH, "NORMAL", List.of(group)))).isEqualTo(OWNER_REQUIRED);
      assertThat(detected(filter(legacyKey, BOTH, "NORMAL", null)))
          .containsExactlyInAnyOrder("나쁜말샘플", "비속어샘플");
    }

    @Test
    @DisplayName("비동기 요청도 접수하기 전에 그룹을 검증해 즉시 오류를 돌려준다")
    void asyncValidatesUpFront() throws Exception {
      Account account = account("async");
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("text", BOTH);
      payload.put("mode", "NORMAL");
      payload.put("callbackUrl", "https://example.com/callback");
      payload.put("whitelistIds", List.of(UUID.randomUUID().toString()));

      MockHttpServletResponse response =
          write(post("/api/v1/filter").header("X-API-KEY", account.apiKey()), null, payload);

      assertThat(code(response)).isEqualTo(NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("결과 캐시는")
  class Cache {

    @Test
    @DisplayName("그룹 없이 받은 결과가 그룹을 지정한 요청에 재사용되지 않는다")
    void plainResultIsNotReusedForGroupRequest() throws Exception {
      Account account = account("cache-a");
      String group = group(account, "게임", "나쁜말샘플");

      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", null))).hasSize(2);
      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(group))))
          .containsExactly("비속어샘플");
    }

    @Test
    @DisplayName("그룹을 지정해 받은 결과가 다른 요청이나 다른 계정에 새지 않는다")
    void groupResultDoesNotLeak() throws Exception {
      Account mine = account("cache-mine");
      Account other = account("cache-other");
      String group = group(mine, "게임", "나쁜말샘플");

      assertThat(detected(filter(mine.apiKey(), BOTH, "NORMAL", List.of(group))))
          .containsExactly("비속어샘플");
      assertThat(detected(filter(mine.apiKey(), BOTH, "NORMAL", null))).hasSize(2);
      assertThat(detected(filter(other.apiKey(), BOTH, "NORMAL", null))).hasSize(2);
      assertThat(detected(filter(SeedApiKeys.READ_CLIENT.apiKey(), BOTH, "NORMAL", null)))
          .hasSize(2);
    }

    @Test
    @DisplayName("그룹을 고치거나 지우면 다음 요청에 바로 반영된다")
    void editsApplyImmediately() throws Exception {
      Account account = account("cache-edit");
      String group = group(account, "게임", "나쁜말샘플");
      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(group))))
          .containsExactly("비속어샘플");

      data(
          write(
              put("/api/v1/dashboard/whitelists/" + group),
              account.login().token(),
              body("게임", List.of("비속어샘플"))));
      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(group))))
          .containsExactly("나쁜말샘플");

      data(request(delete("/api/v1/dashboard/whitelists/" + group), account.login().token()));
      assertThat(code(filter(account.apiKey(), BOTH, "NORMAL", List.of(group))))
          .isEqualTo(NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("계정과 API Key의 변화에는")
  class AccountLifecycle {

    @Test
    @DisplayName("API Key를 재발급해도 새 키로 같은 그룹을 그대로 쓴다")
    void survivesKeyReissue() throws Exception {
      Account account = account("reissue");
      String group = group(account, "게임", "나쁜말샘플");

      JsonNode reissued =
          data(
              request(
                  post("/api/v1/dashboard/keys/" + account.apiKeyId() + "/reissue"),
                  account.login().token()));
      String newKey = reissued.path("apiKey").asText();

      assertThat(newKey).isNotEqualTo(account.apiKey());
      assertThat(detected(filter(newKey, BOTH, "NORMAL", List.of(group)))).containsExactly("비속어샘플");
    }

    @Test
    @DisplayName("한 계정의 여러 API Key가 같은 그룹을 함께 쓴다")
    void sharedAcrossKeysOfOneAccount() throws Exception {
      Account account = account("multi-key");
      String second = issueKey(account.login(), "두 번째 서비스").path("apiKey").asText();
      String group = group(account, "게임", "나쁜말샘플");

      assertThat(detected(filter(account.apiKey(), BOTH, "NORMAL", List.of(group))))
          .containsExactly("비속어샘플");
      assertThat(detected(filter(second, BOTH, "NORMAL", List.of(group)))).containsExactly("비속어샘플");
    }

    @Test
    @DisplayName("과거 키도 발급 이메일로 로그인해 계정에 연결되면 그룹을 쓸 수 있다")
    void legacyKeyBecomesUsableAfterClaim() throws Exception {
      String legacyKey = SeedApiKeys.READ_CLIENT.apiKey();
      Login owner = loginWithEmail("legacy-owner", SeedApiKeys.READ_CLIENT.email());
      String group = data(createGroup(owner, "게임", List.of("나쁜말샘플"))).path("id").asText();

      // 키 연결은 로그인 뒤에 비동기로 일어나므로 연결될 때까지 기다린다.
      long deadline = System.currentTimeMillis() + 10_000;
      while (count(
                  "SELECT COUNT(*) FROM api_keys WHERE user_id IS NOT NULL AND email = '"
                      + SeedApiKeys.READ_CLIENT.email()
                      + "'")
              == 0
          && System.currentTimeMillis() < deadline) {
        Thread.sleep(100);
      }

      assertThat(detected(filter(legacyKey, BOTH, "NORMAL", List.of(group))))
          .containsExactly("비속어샘플");
    }

    @Test
    @DisplayName("비활성화된 계정은 그룹을 관리할 수 없다")
    void disabledAccountCannotManage() throws Exception {
      Account account = account("disabled");
      group(account, "게임", "나쁜말샘플");

      execute(
          "UPDATE users SET status='DISABLED' WHERE id=UNHEX(REPLACE('"
              + account.login().id()
              + "','-',''))");

      assertThat(request(get("/api/v1/dashboard/whitelists"), account.login().token()).getStatus())
          .isIn(401, 403);
    }
  }

  // ---- 지원 코드 ----

  private record Login(String id, String token) {}

  private record Account(Login login, String apiKey, String apiKeyId) {}

  /** 로그인한 계정과 그 계정이 소유한 API Key 하나를 만든다. */
  private Account account(String name) throws Exception {
    Login login = login(name);
    JsonNode issued = issueKey(login, name + " 서비스");
    return new Account(login, issued.path("apiKey").asText(), issued.at("/key/id").asText());
  }

  private JsonNode issueKey(Login login, String name) throws Exception {
    return data(
        write(
            post("/api/v1/dashboard/keys"),
            login.token(),
            Map.of("name", name, "issuerInfo", "whitelist-e2e")));
  }

  private String group(Account account, String name, String... words) throws Exception {
    return data(createGroup(account.login(), name, List.of(words))).path("id").asText();
  }

  private MockHttpServletResponse createGroup(Login login, String name, List<String> words)
      throws Exception {
    return write(post("/api/v1/dashboard/whitelists"), login.token(), body(name, words));
  }

  private Map<String, Object> body(String name, List<String> words) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("name", name);
    payload.put("words", words);
    return payload;
  }

  private JsonNode filter(String apiKey, String text, String mode, List<String> whitelistIds)
      throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("text", text);
    payload.put("mode", mode);
    if (whitelistIds != null) payload.put("whitelistIds", whitelistIds);
    MockHttpServletResponse response =
        write(post("/api/v1/filter").header("X-API-KEY", apiKey), null, payload);
    assertThat(response.getStatus()).isLessThan(500);
    return json(response);
  }

  private List<String> detected(JsonNode response) {
    assertThat(response.at("/status/code").asInt()).isEqualTo(OK);
    List<String> words = new ArrayList<>();
    response
        .path("detected")
        .forEach(
            node -> {
              String word = node.path("filteredWord").asText();
              if (!word.isEmpty()) words.add(word);
            });
    return words;
  }

  private Login login(String name) throws Exception {
    return loginWithEmail(name, name + "@example.test");
  }

  private Login loginWithEmail(String name, String email) throws Exception {
    String exchangeCode =
        loginAuthService.issueExchangeCode(
            new OAuthLoginProfile(
                OAuthProvider.GOOGLE, name, email, true, true, email, name, null));
    JsonNode token = data(write(post("/api/v1/auth/exchange"), null, Map.of("code", exchangeCode)));
    return new Login(token.at("/user/id").asText(), token.path("accessToken").asText());
  }

  private void setRole(String id, String role) throws Exception {
    execute("UPDATE users SET role='" + role + "' WHERE id=UNHEX(REPLACE('" + id + "','-',''))");
  }

  private void execute(String sql) throws Exception {
    try (var connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.executeUpdate(sql);
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

  private List<String> rows(String sql) throws Exception {
    List<String> values = new ArrayList<>();
    try (var connection = dataSource.getConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      while (result.next()) values.add(result.getString(1));
    }
    return values;
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

  private JsonNode json(MockHttpServletResponse response) throws Exception {
    return objectMapper.readTree(
        response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
  }

  private int code(MockHttpServletResponse response) throws Exception {
    return code(json(response));
  }

  private int code(JsonNode body) {
    return body.at("/status/code").asInt();
  }

  private JsonNode data(MockHttpServletResponse response) throws Exception {
    JsonNode body = json(response);
    assertThat(response.getStatus()).as(body.toString()).isEqualTo(200);
    assertThat(code(body)).as(body.toString()).isEqualTo(OK);
    return body.path("data");
  }
}
