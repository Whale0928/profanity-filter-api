package app.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.application.auth.LoginAuthService;
import app.application.auth.LoginRefreshTokenService;
import app.domain.user.OAuthLoginProfile;
import app.domain.user.OAuthProvider;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import app.security.login.SecureOpaqueTokenService;
import app.security.login.SecureOpaqueTokenService.OpaqueToken;
import java.time.Duration;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncodingException;

@Import(LoginAuthTransactionE2ETest.FailingJwtEncoderConfig.class)
class LoginAuthTransactionE2ETest extends AbstractApiTester {

  @Autowired private LoginAuthService loginAuthService;
  @Autowired private LoginRefreshTokenService refreshTokenService;
  @Autowired private SecureOpaqueTokenService opaqueTokenService;
  @Autowired private UserAccountRepository userRepository;
  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("JWT 발급 실패 시 교환 코드 소비와 refresh session 생성을 rollback한다")
  void exchange_whenJwtEncodingFails_rollsBackLoginState() {
    String code =
        loginAuthService.issueExchangeCode(
            new OAuthLoginProfile(
                OAuthProvider.GOOGLE,
                "exchange-rollback-user",
                "exchange-rollback@gmail.com",
                true,
                true,
                "exchange-rollback@gmail.com",
                "Exchange Rollback",
                null));

    assertThatThrownBy(() -> loginAuthService.exchange(code))
        .isInstanceOf(JwtEncodingException.class);

    assertThat(singleColumnIsNull("login_exchange_codes", "consumed_at")).isTrue();
    assertThat(countRows("login_refresh_sessions")).isZero();
    assertThat(countRows("login_refresh_tokens")).isZero();
  }

  @Test
  @DisplayName("JWT 발급 실패 시 refresh rotation을 rollback하고 기존 token을 유지한다")
  void refresh_whenJwtEncodingFails_rollsBackRotation() {
    Instant now = Instant.now();
    UserAccount user =
        userRepository.save(
            UserAccount.create("Refresh Rollback", "refresh-rollback@example.com", null, now));
    OpaqueToken currentToken = opaqueTokenService.generate();
    refreshTokenService.createSession(
        user.getId(), currentToken.hash(), now, Duration.ofDays(14), Duration.ofDays(30));

    assertThatThrownBy(() -> loginAuthService.refresh(currentToken.plaintext()))
        .isInstanceOf(JwtEncodingException.class);

    assertThat(countRows("login_refresh_tokens")).isEqualTo(1);
    assertThat(singleColumnIsNull("login_refresh_tokens", "consumed_at")).isTrue();
    assertThat(singleColumnIsNull("login_refresh_sessions", "revoked_at")).isTrue();
  }

  private long countRows(String table) {
    try (var connection = dataSource.getConnection();
        var statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
        var result = statement.executeQuery()) {
      result.next();
      return result.getLong(1);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to count login auth rows", exception);
    }
  }

  private boolean singleColumnIsNull(String table, String column) {
    try (var connection = dataSource.getConnection();
        var statement = connection.prepareStatement("SELECT " + column + " FROM " + table);
        var result = statement.executeQuery()) {
      assertThat(result.next()).isTrue();
      Object value = result.getObject(1);
      assertThat(result.next()).isFalse();
      return value == null;
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to read login auth state", exception);
    }
  }

  @TestConfiguration
  static class FailingJwtEncoderConfig {
    @Bean
    @Primary
    JwtEncoder failingLoginJwtEncoder() {
      return parameters -> {
        throw new JwtEncodingException("simulated login JWT encoding failure");
      };
    }
  }
}
