package app.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private EmailService emailService;

    @Nested
    @DisplayName("이메일 인증 코드를 검증할 때")
    class VerifyEmailCode {

        @Test
        @DisplayName("저장된 코드와 입력된 코드가 일치하면 true를 반환한다")
        void returnsTrueWhenCodesMatch() {
            // given
            String email = "test@example.com";
            String code = "123456";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("email:" + email)).thenReturn(code);

            // when
            boolean result = emailService.verifyEmailCode(email, code);

            // then
            assertTrue(result);
            verify(redisTemplate).delete("email:" + email);
        }

        @Test
        @DisplayName("저장된 코드와 입력된 코드가 일치하지 않으면 false를 반환한다")
        void returnsFalseWhenCodesDontMatch() {
            // given
            String email = "test@example.com";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("email:" + email)).thenReturn("654321");

            // when
            boolean result = emailService.verifyEmailCode(email, "123456");

            // then
            assertFalse(result);
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("저장된 코드가 없으면 false를 반환한다")
        void returnsFalseWhenNoStoredCode() {
            // given
            String email = "test@example.com";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("email:" + email)).thenReturn(null);

            // when
            boolean result = emailService.verifyEmailCode(email, "123456");

            // then
            assertFalse(result);
            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("이메일 인증 코드를 생성할 때")
    class CreateEmailVerificationCode {

        @Test
        @DisplayName("6자리 숫자 코드를 생성하고 Redis에 저장한다")
        void createsAndStoresSixDigitCode() {
            // given
            String email = "test@example.com";
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            // when
            String code = emailService.createEmailVerificationCode(email);

            // then
            assertNotNull(code);
            assertEquals(6, code.length());
            assertTrue(code.matches("\\d{6}"));
            verify(valueOperations).set(eq("email:" + email), anyString(), any(Duration.class));
        }
    }
}
