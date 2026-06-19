package app.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import app.core.data.response.ApiResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ResponseCustomizingAdviceTest {

  private final ResponseCustomizingAdvice advice =
      new ResponseCustomizingAdvice(List.of(new HostResponseCustomizer("api.kr-filter.com")));

  private Object invoke(Object body, String host) {
    MockHttpServletRequest servlet = new MockHttpServletRequest();
    servlet.setServerName(host);
    ServerHttpRequest request = new ServletServerHttpRequest(servlet);
    ServerHttpResponse responseOut = new ServletServerHttpResponse(new MockHttpServletResponse());
    return advice.beforeBodyWrite(
        body, null, MediaType.APPLICATION_JSON, null, request, responseOut);
  }

  @Nested
  @DisplayName("ApiResponse 응답일 때")
  class ApiResponseBody {

    @Test
    @DisplayName("프록시 호스트면 meta에 커스텀이 적용된다")
    void appliesForProxiedHost() {
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();

      invoke(body, "api.kr-filter.com");

      assertThat(body.meta()).containsEntry("servedVia", "api.kr-filter.com");
    }

    @Test
    @DisplayName("기존 도메인 호스트면 meta가 비어 있다(기존 응답 불변)")
    void noChangeForLegacyHost() {
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();

      invoke(body, "api.profanity.kr-filter.com");

      assertThat(body.meta()).isEmpty();
    }

    @Test
    @DisplayName("원본 body 인스턴스를 그대로 반환한다")
    void returnsSameBody() {
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();

      Object result = invoke(body, "api.kr-filter.com");

      assertThat(result).isSameAs(body);
    }
  }

  @Nested
  @DisplayName("ApiResponse가 아닌 응답일 때")
  class NonApiResponseBody {

    @Test
    @DisplayName("문자열 body는 가공 없이 그대로 반환한다")
    void passThroughString() {
      Object result = invoke("just a string", "api.kr-filter.com");

      assertThat(result).isEqualTo("just a string");
    }

    @Test
    @DisplayName("null body도 안전하게 통과한다")
    void passThroughNull() {
      Object result = invoke(null, "api.kr-filter.com");

      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("커스터마이저 구성에 따라")
  class CustomizerComposition {

    @Test
    @DisplayName("등록된 커스터마이저가 없으면 meta는 비어 있다")
    void noCustomizers() {
      ResponseCustomizingAdvice empty = new ResponseCustomizingAdvice(List.of());
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();

      MockHttpServletRequest servlet = new MockHttpServletRequest();
      servlet.setServerName("api.kr-filter.com");
      empty.beforeBodyWrite(
          body,
          null,
          MediaType.APPLICATION_JSON,
          null,
          new ServletServerHttpRequest(servlet),
          new ServletServerHttpResponse(new MockHttpServletResponse()));

      assertThat(body.meta()).isEmpty();
    }

    @Test
    @DisplayName("조건이 맞는 여러 커스터마이저가 모두 적용된다")
    void multipleCustomizers() {
      ResponseCustomizer extra =
          new ResponseCustomizer() {
            @Override
            public boolean supports(RequestContext context) {
              return true;
            }

            @Override
            public void customize(Map<String, Object> meta, RequestContext context) {
              meta.put("extra", true);
            }
          };
      ResponseCustomizingAdvice multi =
          new ResponseCustomizingAdvice(
              List.of(new HostResponseCustomizer("api.kr-filter.com"), extra));
      ApiResponse<String> body = ApiResponse.ok("payload").getBody();

      MockHttpServletRequest servlet = new MockHttpServletRequest();
      servlet.setServerName("api.kr-filter.com");
      multi.beforeBodyWrite(
          body,
          null,
          MediaType.APPLICATION_JSON,
          null,
          new ServletServerHttpRequest(servlet),
          new ServletServerHttpResponse(new MockHttpServletResponse()));

      assertThat(body.meta())
          .containsEntry("servedVia", "api.kr-filter.com")
          .containsEntry("extra", true);
    }
  }
}
