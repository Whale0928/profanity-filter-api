package app.core.data.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Nested
  @DisplayName("meta 직렬화(하위호환)를 검증할 때")
  class MetaSerialization {

    @Test
    @DisplayName("meta가 비어 있으면 JSON에 meta 키가 아예 없다")
    void emptyMetaIsOmitted() throws Exception {
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();

      String json = objectMapper.writeValueAsString(body);

      assertThat(json).doesNotContain("meta");
    }

    @Test
    @DisplayName("error 응답(meta 비어 있음)도 meta 키가 없다")
    void errorResponseOmitsMeta() throws Exception {
      ApiResponse<Object> body =
          ApiResponse.<Object>error(
                  Status.of(app.core.data.response.constant.StatusCode.BAD_REQUEST))
              .getBody();

      String json = objectMapper.writeValueAsString(body);

      assertThat(json).doesNotContain("meta");
    }

    @Test
    @DisplayName("meta에 값이 있으면 JSON에 meta가 포함된다")
    void nonEmptyMetaIsIncluded() throws Exception {
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();
      body.meta().put("servedVia", "api.kr-filter.com");

      String json = objectMapper.writeValueAsString(body);

      assertThat(json).contains("\"meta\"").contains("servedVia").contains("api.kr-filter.com");
    }
  }

  @Nested
  @DisplayName("meta 컨테이너를 검증할 때")
  class MetaContainer {

    @Test
    @DisplayName("ok 응답의 meta는 비어 있지만 null이 아니다")
    void metaIsEmptyButNotNull() {
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();

      assertThat(body.meta()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("meta는 가변이라 커스터마이저가 값을 추가할 수 있다")
    void metaIsMutable() {
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();

      body.meta().put("k", "v");

      assertThat(body.meta()).containsEntry("k", "v");
    }

    @Test
    @DisplayName("응답마다 독립된 meta 인스턴스를 가진다")
    void metaIsNotShared() {
      ApiResponse<String> first = ApiResponse.ok("a").getBody();
      ApiResponse<String> second = ApiResponse.ok("b").getBody();

      first.meta().put("only", "first");

      assertThat(second.meta()).isEmpty();
    }
  }
}
