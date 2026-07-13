package app.security.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import app.application.client.MetadataReader;
import app.domain.client.ClientMetadata;
import app.security.filter.RequestCredential;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class ApiKeyAuthenticatorTest {

  @Test
  @DisplayName("API Key 검증 결과에 인증 타입 authority와 기존 권한을 함께 설정한다")
  void authenticate_validApiKey_returnsTypedAuthenticationWithLegacyAuthorities() {
    UUID clientId = UUID.randomUUID();
    MetadataReader metadataReader =
        new MetadataReader() {
          @Override
          public ClientMetadata read(String apiKey) {
            return new ClientMetadata(
                clientId,
                "client@example.com",
                "test client",
                null,
                List.of("READ", "WRITE"),
                "2026-07-11T00:00:00Z");
          }

          @Override
          public String getApiKeyByEmail(String email) {
            throw new UnsupportedOperationException();
          }

          @Override
          public boolean verifyClientByEmail(String email) {
            throw new UnsupportedOperationException();
          }
        };
    ApiKeyAuthenticator authenticator = new ApiKeyAuthenticator(metadataReader);

    Authentication authentication =
        authenticator.authenticate(
            new RequestCredential(AuthenticationType.API_KEY, "secret-api-key"));

    assertThat(authentication.getPrincipal()).isInstanceOf(ApiKeyPrincipal.class);
    assertThat(((ApiKeyPrincipal) authentication.getPrincipal()).id()).isEqualTo(clientId);
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("AUTH_API_KEY", "ROLE_READ", "ROLE_WRITE");
    assertThat(authentication.getCredentials()).isEqualTo("secret-api-key");
    assertThat(authentication.toString()).doesNotContain("secret-api-key");
  }
}
