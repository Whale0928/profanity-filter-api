package app.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

class GitHubOAuth2UserServiceTest {
  private static final String ACCESS_TOKEN = "github-access-token";

  @Test
  @DisplayName("GitHub 기본 프로필과 검증된 대표 이메일을 하나의 사용자 속성으로 합친다")
  void loadUser_whenVerifiedPrimaryEmailExists_enrichesProfile() throws Exception {
    try (FakeGitHubServer server =
        new FakeGitHubServer(
            """
            [
              {"email":"secondary@example.com","primary":false,"verified":true},
              {"email":"primary@example.com","primary":true,"verified":true}
            ]
            """)) {
      GitHubOAuth2UserService service = new GitHubOAuth2UserService();

      OAuth2User user = service.loadUser(userRequest(server.baseUrl()));

      assertThat(user.getName()).isEqualTo("12345");
      assertThat(user.<String>getAttribute("login")).isEqualTo("hgkim");
      assertThat(user.<String>getAttribute("email")).isEqualTo("primary@example.com");
      assertThat(user.<Boolean>getAttribute("email_verified")).isTrue();
      assertThat(server.profileAuthorization()).isEqualTo("Bearer " + ACCESS_TOKEN);
      assertThat(server.emailAuthorization()).isEqualTo("Bearer " + ACCESS_TOKEN);
    }
  }

  @Test
  @DisplayName("GitHub에 검증된 대표 이메일이 없으면 로그인을 거부한다")
  void loadUser_whenVerifiedPrimaryEmailIsMissing_throwsOAuth2AuthenticationException()
      throws Exception {
    try (FakeGitHubServer server =
        new FakeGitHubServer(
            """
            [
              {"email":"primary@example.com","primary":true,"verified":false},
              {"email":"secondary@example.com","primary":false,"verified":true}
            ]
            """)) {
      GitHubOAuth2UserService service = new GitHubOAuth2UserService();

      assertThatThrownBy(() -> service.loadUser(userRequest(server.baseUrl())))
          .isInstanceOf(OAuth2AuthenticationException.class)
          .hasMessageContaining("invalid_user_info_response");
    }
  }

  private OAuth2UserRequest userRequest(String baseUrl) {
    ClientRegistration registration =
        ClientRegistration.withRegistrationId("github")
            .clientId("client-id")
            .clientSecret("client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(baseUrl + "/login/oauth2/code/github")
            .scope("read:user", "user:email")
            .authorizationUri(baseUrl + "/oauth2/authorize")
            .tokenUri(baseUrl + "/oauth2/token")
            .userInfoUri(baseUrl + "/user")
            .userNameAttributeName("id")
            .clientName("GitHub")
            .build();
    Instant issuedAt = Instant.now();
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            ACCESS_TOKEN,
            issuedAt,
            issuedAt.plusSeconds(300),
            Set.of("read:user", "user:email"));
    return new OAuth2UserRequest(registration, accessToken);
  }

  private static final class FakeGitHubServer implements AutoCloseable {
    private final HttpServer server;
    private final AtomicReference<String> profileAuthorization = new AtomicReference<>();
    private final AtomicReference<String> emailAuthorization = new AtomicReference<>();

    private FakeGitHubServer(String emailResponse) throws IOException {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/user/emails", exchange -> respond(exchange, emailResponse, emailAuthorization));
      server.createContext(
          "/user",
          exchange ->
              respond(
                  exchange,
                  """
                  {
                    "id": 12345,
                    "login": "hgkim",
                    "name": "HG Kim",
                    "email": null,
                    "avatar_url": "https://example.com/avatar.png"
                  }
                  """,
                  profileAuthorization));
      server.start();
    }

    private String baseUrl() {
      return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private String profileAuthorization() {
      return profileAuthorization.get();
    }

    private String emailAuthorization() {
      return emailAuthorization.get();
    }

    private void respond(
        HttpExchange exchange, String responseBody, AtomicReference<String> authorization)
        throws IOException {
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      try (var output = exchange.getResponseBody()) {
        output.write(body);
      }
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }
}
