package app.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpClientTest {

    @Nested
    @DisplayName("클라이언트 IP를 조회할 때")
    class GetClientIPTest {
        private final MockHttpServletRequest request = new MockHttpServletRequest();

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있다면 해당 IP를 반환한다")
        void test1() {
            // given
            String expectedIP = "192.168.0.1";
            request.addHeader("X-Forwarded-For", expectedIP);

            // when
            String clientIP = HttpClient.getClientIP(request);

            // then
            assertEquals(expectedIP, clientIP);
        }

        @Test
        @DisplayName("설정된 헤더 타입을 순차적으로 확인하여 첫번째 유효한 IP를 반환한다")
        void test2() {
            // given
            String expectedIP = "192.168.0.2";
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("Proxy-Client-IP", expectedIP);
            request.addHeader("WL-Proxy-Client-IP", "192.168.0.3");

            // when
            String clientIP = HttpClient.getClientIP(request);

            // then
            assertEquals(expectedIP, clientIP);
        }

        @Test
        @DisplayName("모든 헤더가 null이면 RemoteAddr을 반환한다")
        void test3() {
            // given
            String expectedIP = "127.0.0.1";
            request.setRemoteAddr(expectedIP);

            // when
            String clientIP = HttpClient.getClientIP(request);

            // then
            assertEquals(expectedIP, clientIP);
        }

        @Test
        @DisplayName("모든 헤더가 비어있으면 RemoteAddr을 반환한다")
        void test4() {
            // given
            String expectedIP = "127.0.0.1";
            request.setRemoteAddr(expectedIP);
            request.addHeader("X-Forwarded-For", "");
            request.addHeader("Proxy-Client-IP", "");
            request.addHeader("WL-Proxy-Client-IP", "");
            request.addHeader("HTTP_CLIENT_IP", "");
            request.addHeader("HTTP_X_FORWARDED_FOR", "");

            // when
            String clientIP = HttpClient.getClientIP(request);

            // then
            assertEquals(expectedIP, clientIP);
        }

        @Test
        @DisplayName("모든 헤더가 unknown이면 RemoteAddr을 반환한다")
        void test5() {
            // given
            String expectedIP = "127.0.0.1";
            request.setRemoteAddr(expectedIP);
            request.addHeader("X-Forwarded-For", "unknown");
            request.addHeader("Proxy-Client-IP", "unknown");
            request.addHeader("WL-Proxy-Client-IP", "unknown");
            request.addHeader("HTTP_CLIENT_IP", "unknown");
            request.addHeader("HTTP_X_FORWARDED_FOR", "unknown");

            // when
            String clientIP = HttpClient.getClientIP(request);

            // then
            assertEquals(expectedIP, clientIP);
        }
    }

    @Nested
    @DisplayName("Referrer 정보를 조회할 때")
    class GetReferrerTest {
        private final MockHttpServletRequest request = new MockHttpServletRequest();

        @Test
        @DisplayName("Referer 헤더가 있다면 해당 값을 반환한다")
        void test1() {
            // given
            String expectedReferrer = "https://example.com";
            request.addHeader("Referer", expectedReferrer);

            // when
            String referrer = HttpClient.getReferrer(request);

            // then
            assertEquals(expectedReferrer, referrer);
        }

        @Test
        @DisplayName("Referer 헤더가 없다면 null을 반환한다")
        void test2() {
            // when
            String referrer = HttpClient.getReferrer(request);

            // then
            assertNull(referrer);
        }
    }
}
