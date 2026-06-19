package app.application;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpClient {

  private static final Logger log = LogManager.getLogger(HttpClient.class);
  // Cloudflare 프록시가 실어주는 실제 클라이언트 IP (단일 값)
  // 주의: origin이 Cloudflare IP 대역만 허용해야 위조를 막을 수 있다
  private static final String CF_CONNECTING_IP = "CF-Connecting-IP";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";
  private static final String[] FALLBACK_HEADERS = {
    "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
  };

  public static String getClientIP(HttpServletRequest request) {
    // 1) Cloudflare 프록시 경유 시 실제 클라이언트 IP
    String ip = request.getHeader(CF_CONNECTING_IP);

    // 2) X-Forwarded-For: "client, proxy1, ..." 형식이라 첫 IP가 원본 클라이언트
    if (isBlank(ip)) {
      String xff = request.getHeader(X_FORWARDED_FOR);
      if (!isBlank(xff)) {
        ip = xff.split(",")[0].trim();
      }
    }

    // 3) 기타 프록시 헤더
    if (isBlank(ip)) {
      for (String header : FALLBACK_HEADERS) {
        String candidate = request.getHeader(header);
        if (!isBlank(candidate)) {
          ip = candidate;
          break;
        }
      }
    }

    // 4) 직접 연결 시 소켓 주소
    if (isBlank(ip)) {
      ip = request.getRemoteAddr();
    }

    log.info("실제 원격(클라이언트) IP 주소: {}", ip);
    return ip;
  }

  private static boolean isBlank(String value) {
    return Objects.isNull(value) || value.isEmpty() || "unknown".equalsIgnoreCase(value);
  }

  public static String getReferrer(HttpServletRequest request) {
    String referrer = request.getHeader("Referer");
    log.info("Referer : {}", referrer);
    return referrer;
  }
}
