package app.application.client;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.client.TemporaryApiKey;
import app.domain.client.TemporaryApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemporaryApiKeyServiceTest {

    @Mock
    private TemporaryApiKeyRepository temporaryApiKeyRepository;

    private TemporaryApiKeyService temporaryApiKeyService;

    @BeforeEach
    void setUp() {
        temporaryApiKeyService = new TemporaryApiKeyService(temporaryApiKeyRepository);
    }

    @Test
    @DisplayName("임시 API 키 발급 성공")
    void issueTemporaryKey_Success() {
        // Given
        String ipAddress = "192.168.1.1";
        when(temporaryApiKeyRepository.countTodayIssuancesByIp(ipAddress)).thenReturn(0);
        doNothing().when(temporaryApiKeyRepository).save(any(TemporaryApiKey.class));
        doNothing().when(temporaryApiKeyRepository).incrementIpIssuanceCount(ipAddress);

        // When
        TemporaryApiKey result = temporaryApiKeyService.issueTemporaryKey(ipAddress);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApiKey()).startsWith("temp_");
        assertThat(result.getIpAddress()).isEqualTo(ipAddress);
        assertThat(result.getRemainingCount()).isEqualTo(10);
        assertThat(result.getIssuedAt()).isNotNull();
        assertThat(result.getExpiredAt()).isNotNull();

        verify(temporaryApiKeyRepository, times(1)).save(any(TemporaryApiKey.class));
        verify(temporaryApiKeyRepository, times(1)).incrementIpIssuanceCount(ipAddress);
    }

    @Test
    @DisplayName("임시 API 키 발급 실패 - IP 발급 제한 초과")
    void issueTemporaryKey_ExceedLimit() {
        // Given
        String ipAddress = "192.168.1.1";
        when(temporaryApiKeyRepository.countTodayIssuancesByIp(ipAddress)).thenReturn(3);

        // When & Then
        assertThatThrownBy(() -> temporaryApiKeyService.issueTemporaryKey(ipAddress))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("일일 임시 키 발급 제한을 초과했습니다");

        verify(temporaryApiKeyRepository, times(1)).countTodayIssuancesByIp(ipAddress);
        verify(temporaryApiKeyRepository, times(0)).save(any(TemporaryApiKey.class));
    }

    @Test
    @DisplayName("임시 API 키 검증 성공")
    void validateAndUse_Success() {
        // Given
        String apiKey = "temp_test123";
        TemporaryApiKey tempKey = TemporaryApiKey.builder()
                .apiKey(apiKey)
                .ipAddress("192.168.1.1")
                .remainingCount(5)
                .issuedAt(java.time.LocalDateTime.now())
                .expiredAt(java.time.LocalDateTime.now().plusHours(24))
                .build();

        when(temporaryApiKeyRepository.findByApiKey(apiKey)).thenReturn(Optional.of(tempKey));
        doNothing().when(temporaryApiKeyRepository).save(any(TemporaryApiKey.class));

        // When
        boolean result = temporaryApiKeyService.validateAndUse(apiKey);

        // Then
        assertThat(result).isTrue();
        assertThat(tempKey.getRemainingCount()).isEqualTo(4);
        verify(temporaryApiKeyRepository, times(1)).save(tempKey);
    }

    @Test
    @DisplayName("임시 API 키 검증 실패 - 존재하지 않는 키")
    void validateAndUse_NotFound() {
        // Given
        String apiKey = "temp_notexist";
        when(temporaryApiKeyRepository.findByApiKey(apiKey)).thenReturn(Optional.empty());

        // When
        boolean result = temporaryApiKeyService.validateAndUse(apiKey);

        // Then
        assertThat(result).isFalse();
        verify(temporaryApiKeyRepository, times(0)).save(any(TemporaryApiKey.class));
    }

    @Test
    @DisplayName("임시 API 키 사용 횟수 소진 시 삭제")
    void validateAndUse_DeleteWhenExhausted() {
        // Given
        String apiKey = "temp_test123";
        TemporaryApiKey tempKey = TemporaryApiKey.builder()
                .apiKey(apiKey)
                .ipAddress("192.168.1.1")
                .remainingCount(1)
                .issuedAt(java.time.LocalDateTime.now())
                .expiredAt(java.time.LocalDateTime.now().plusHours(24))
                .build();

        when(temporaryApiKeyRepository.findByApiKey(apiKey)).thenReturn(Optional.of(tempKey));
        doNothing().when(temporaryApiKeyRepository).save(any(TemporaryApiKey.class));
        doNothing().when(temporaryApiKeyRepository).delete(apiKey);

        // When
        boolean result = temporaryApiKeyService.validateAndUse(apiKey);

        // Then
        assertThat(result).isTrue();
        assertThat(tempKey.getRemainingCount()).isEqualTo(0);
        verify(temporaryApiKeyRepository, times(1)).save(tempKey);
        verify(temporaryApiKeyRepository, times(1)).delete(apiKey);
    }
}
