package app.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@Slf4j
@RequiredArgsConstructor
public class CookieOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String COOKIE_SAME_SITE = "Lax";
  private static final String COOKIE_PATH = "/";
  private static final String VALUE_SEPARATOR = ".";

  private final SsoCookieProperties properties;

  /**
   * callback 요청에서 OAuth2 authorization request 쿠키를 읽는다.
   *
   * <p>쿠키 서명과 TTL 검증을 통과한 경우에만 Spring Security가 state 비교에 사용할 요청 객체를 반환한다.
   */
  @Override
  @Nullable
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return findCookie(request, properties.name())
        .map(Cookie::getValue)
        .map(this::decode)
        .orElse(null);
  }

  /**
   * 로그인 시작 시점의 OAuth2 authorization request를 signed cookie로 저장한다.
   *
   * <p>요청 객체와 생성 시각을 직렬화한 뒤 HMAC 서명을 붙여 브라우저 쿠키로 내려보낸다.
   */
  @Override
  public void saveAuthorizationRequest(
      @Nullable OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorizationRequest == null) {
      expireCookie(request, response);
      return;
    }

    StoredAuthorizationRequest storedAuthorizationRequest =
        new StoredAuthorizationRequest(Instant.now().getEpochSecond(), authorizationRequest);
    String payload =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(serialize(storedAuthorizationRequest));
    String signature = sign(payload);
    addCookie(request, response, payload + VALUE_SEPARATOR + signature, properties.ttlSeconds());
  }

  /**
   * callback 처리 후 저장된 OAuth2 authorization request를 읽고 쿠키를 만료시킨다.
   *
   * <p>state 검증에 한 번 사용한 임시 쿠키가 다음 로그인 흐름에 재사용되지 않도록 즉시 삭제한다.
   */
  @Override
  @Nullable
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
    expireCookie(request, response);
    return authorizationRequest;
  }

  /**
   * 요청 쿠키 목록에서 설정된 이름과 일치하는 쿠키를 찾는다.
   *
   * <p>브라우저가 쿠키를 보내지 않았거나 이름이 맞는 쿠키가 없으면 빈 Optional을 반환한다.
   */
  private java.util.Optional<Cookie> findCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return java.util.Optional.empty();
    }

    for (Cookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        return java.util.Optional.of(cookie);
      }
    }
    return java.util.Optional.empty();
  }

  /**
   * 쿠키 값을 검증하고 OAuth2 authorization request로 복원한다.
   *
   * <p>payload와 signature를 분리해 서명을 확인하고, 만료 시간이 지난 쿠키는 인증 흐름에서 제외한다.
   */
  @Nullable
  private OAuth2AuthorizationRequest decode(String cookieValue) {
    String[] parts = cookieValue.split("\\" + VALUE_SEPARATOR, 2);
    if (parts.length != 2 || !isValidSignature(parts[0], parts[1])) {
      log.warn("Invalid OAuth2 authorization request cookie signature");
      return null;
    }

    try {
      byte[] decoded = Base64.getUrlDecoder().decode(parts[0]);
      Object deserialized = deserialize(decoded);
      if (!(deserialized
          instanceof
          StoredAuthorizationRequest(
              long createdAtEpochSecond,
              OAuth2AuthorizationRequest authorizationRequest))) {
        return null;
      }
      if (isExpired(createdAtEpochSecond)) {
        log.warn("Expired OAuth2 authorization request cookie");
        return null;
      }
      return authorizationRequest;
    } catch (IllegalArgumentException exception) {
      log.warn("Failed to decode OAuth2 authorization request cookie", exception);
      return null;
    }
  }

  /**
   * 쿠키에 담을 저장 객체를 Java 직렬화 바이트 배열로 변환한다.
   *
   * <p>Spring Security의 OAuth2AuthorizationRequest 전체를 callback 시점에 복원하기 위한 내부 표현이다.
   */
  private byte[] serialize(StoredAuthorizationRequest storedAuthorizationRequest) {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeObject(storedAuthorizationRequest);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Failed to serialize OAuth2 authorization request", exception);
    }
  }

  /**
   * 쿠키 payload의 직렬화 바이트 배열을 Java 객체로 되돌린다.
   *
   * <p>복원 실패는 쿠키 값이 깨졌거나 기대한 형식이 아니라는 의미이므로 잘못된 요청으로 처리한다.
   */
  private Object deserialize(byte[] value) {
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
      return objectInputStream.readObject();
    } catch (IOException | ClassNotFoundException exception) {
      throw new IllegalArgumentException(
          "Failed to deserialize OAuth2 authorization request", exception);
    }
  }

  /**
   * 저장 시각 기준으로 쿠키 TTL을 초과했는지 확인한다.
   *
   * <p>OAuth2 state는 로그인 중간에만 필요한 값이므로 짧은 시간 안에 사용되지 않으면 폐기한다.
   */
  private boolean isExpired(long createdAtEpochSecond) {
    long elapsedSeconds = Instant.now().getEpochSecond() - createdAtEpochSecond;
    return elapsedSeconds > properties.ttlSeconds();
  }

  /**
   * payload와 함께 전달된 서명이 서버에서 다시 계산한 서명과 같은지 비교한다.
   *
   * <p>MessageDigest.isEqual을 사용해 단순 문자열 비교보다 timing attack에 덜 민감하게 검증한다.
   */
  private boolean isValidSignature(String payload, String signature) {
    byte[] expected = sign(payload).getBytes(StandardCharsets.UTF_8);
    byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expected, actual);
  }

  /**
   * 서버 signing key로 payload의 HMAC-SHA256 서명을 만든다.
   *
   * <p>브라우저는 signing key를 모르기 때문에 payload를 조작해도 유효한 서명을 새로 만들 수 없다.
   */
  private String sign(String payload) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(
          new SecretKeySpec(
              properties.signingKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Failed to sign OAuth2 authorization request cookie", exception);
    }
  }

  /**
   * OAuth2 authorization request 쿠키를 응답에 추가한다.
   *
   * <p>HttpOnly와 SameSite=Lax를 적용하고, 현재 요청이 HTTPS일 때만 Secure 속성을 붙인다.
   */
  private void addCookie(
      HttpServletRequest request, HttpServletResponse response, String value, long maxAgeSeconds) {
    ResponseCookie cookie =
        ResponseCookie.from(properties.name(), value)
            .httpOnly(true)
            .secure(request.isSecure())
            .sameSite(COOKIE_SAME_SITE)
            .path(COOKIE_PATH)
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  /**
   * 같은 이름의 쿠키를 maxAge 0으로 내려보내 브라우저에서 삭제한다.
   *
   * <p>성공, 실패, null 저장 상황에서 남은 authorization request 쿠키를 정리하는 데 사용한다.
   */
  private void expireCookie(HttpServletRequest request, HttpServletResponse response) {
    addCookie(request, response, "", 0);
  }

  private record StoredAuthorizationRequest(
      long createdAtEpochSecond, OAuth2AuthorizationRequest authorizationRequest)
      implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
  }
}
