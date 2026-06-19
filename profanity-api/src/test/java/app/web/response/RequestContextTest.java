package app.web.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestContextTest {

  @Test
  @DisplayName("요청에서 호스트를 추출한다")
  void extractsHost() {
    MockHttpServletRequest servlet = new MockHttpServletRequest();
    servlet.setServerName("api.kr-filter.com");

    RequestContext context = RequestContext.from(new ServletServerHttpRequest(servlet));

    assertThat(context.host()).isEqualTo("api.kr-filter.com");
  }

  @Test
  @DisplayName("기존 도메인 호스트도 그대로 추출한다")
  void extractsLegacyHost() {
    MockHttpServletRequest servlet = new MockHttpServletRequest();
    servlet.setServerName("api.profanity.kr-filter.com");

    RequestContext context = RequestContext.from(new ServletServerHttpRequest(servlet));

    assertThat(context.host()).isEqualTo("api.profanity.kr-filter.com");
  }

  @Test
  @DisplayName("대문자 호스트는 소문자로 정규화한다")
  void normalizesUppercaseHost() {
    MockHttpServletRequest servlet = new MockHttpServletRequest();
    servlet.setServerName("API.KR-FILTER.COM");

    RequestContext context = RequestContext.from(new ServletServerHttpRequest(servlet));

    assertThat(context.host()).isEqualTo("api.kr-filter.com");
  }
}
