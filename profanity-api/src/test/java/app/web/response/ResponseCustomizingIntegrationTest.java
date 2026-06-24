package app.web.response;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.core.data.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 실제 MockMvc 직렬화 흐름(@RestControllerAdvice 등록 → supports → 직렬화)까지 검증하는 통합 테스트. */
@WebMvcTest(
    controllers = ResponseCustomizingIntegrationTest.MetaTestController.class,
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({ResponseCustomizingAdvice.class, HostResponseCustomizer.class})
@TestPropertySource(properties = "app.response.proxied-host=api.kr-filter.com")
class ResponseCustomizingIntegrationTest {

  @Autowired private MockMvc mockMvc;

  private static RequestPostProcessor host(String host) {
    return request -> {
      request.setServerName(host);
      return request;
    };
  }

  @Test
  @DisplayName("프록시 호스트로 오면 응답 JSON에 meta.servedVia가 포함된다")
  void proxiedHostIncludesMeta() throws Exception {
    mockMvc
        .perform(get("/__test/meta").with(host("api.kr-filter.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.servedVia").value("api.kr-filter.com"));
  }

  @Test
  @DisplayName("기존 도메인 호스트로 오면 meta 키가 없다(기존 응답 불변)")
  void legacyHostHasNoMeta() throws Exception {
    mockMvc
        .perform(get("/__test/meta").with(host("api.profanity.kr-filter.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta").doesNotExist());
  }

  @Test
  @DisplayName("대문자 호스트도 정규화되어 meta가 포함된다")
  void uppercaseHostNormalized() throws Exception {
    mockMvc
        .perform(get("/__test/meta").with(host("API.KR-FILTER.COM")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meta.servedVia").value("api.kr-filter.com"));
  }

  @RestController
  static class MetaTestController {
    @GetMapping("/__test/meta")
    ResponseEntity<ApiResponse<String>> meta() {
      return ApiResponse.ok("hello");
    }
  }
}
