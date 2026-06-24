package app.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HostResponseCustomizerTest {

  private final HostResponseCustomizer customizer = new HostResponseCustomizer("api.kr-filter.com");

  @Nested
  @DisplayName("supports 조건을 검증할 때")
  class Supports {

    @Test
    @DisplayName("프록시 호스트면 true")
    void proxiedHost() {
      assertThat(customizer.supports(new RequestContext("api.kr-filter.com"))).isTrue();
    }

    @Test
    @DisplayName("기존 도메인 호스트면 false")
    void legacyHost() {
      assertThat(customizer.supports(new RequestContext("api.profanity.kr-filter.com"))).isFalse();
    }

    @Test
    @DisplayName("관계없는 호스트면 false")
    void otherHost() {
      assertThat(customizer.supports(new RequestContext("evil.example.com"))).isFalse();
    }

    @Test
    @DisplayName("host가 null이면 false")
    void nullHost() {
      assertThat(customizer.supports(new RequestContext(null))).isFalse();
    }

    @Test
    @DisplayName("context 자체가 null이면 false")
    void nullContext() {
      assertThat(customizer.supports(null)).isFalse();
    }

    @Test
    @DisplayName("설정 호스트가 대문자여도 정규화되어 매칭된다")
    void caseInsensitiveConfig() {
      HostResponseCustomizer upper = new HostResponseCustomizer("API.KR-FILTER.COM");

      assertThat(upper.supports(new RequestContext("api.kr-filter.com"))).isTrue();
    }

    @Test
    @DisplayName("프록시 호스트 설정이 비어 있으면 비활성화된다(false)")
    void disabledWhenBlank() {
      HostResponseCustomizer disabled = new HostResponseCustomizer("");

      assertThat(disabled.supports(new RequestContext("api.kr-filter.com"))).isFalse();
    }

    @Test
    @DisplayName("프록시 호스트 설정이 null이면 비활성화된다(false)")
    void disabledWhenNull() {
      HostResponseCustomizer disabled = new HostResponseCustomizer(null);

      assertThat(disabled.supports(new RequestContext("api.kr-filter.com"))).isFalse();
    }
  }

  @Nested
  @DisplayName("customize 동작을 검증할 때")
  class Customize {

    @Test
    @DisplayName("servedVia를 meta에 넣는다")
    void putsServedVia() {
      Map<String, Object> meta = new HashMap<>();

      customizer.customize(meta, new RequestContext("api.kr-filter.com"));

      assertThat(meta).containsEntry("servedVia", "api.kr-filter.com");
    }

    @Test
    @DisplayName("기존 meta 항목을 보존한다")
    void preservesExistingEntries() {
      Map<String, Object> meta = new HashMap<>();
      meta.put("existing", "value");

      customizer.customize(meta, new RequestContext("api.kr-filter.com"));

      assertThat(meta)
          .containsEntry("existing", "value")
          .containsEntry("servedVia", "api.kr-filter.com");
    }
  }
}
