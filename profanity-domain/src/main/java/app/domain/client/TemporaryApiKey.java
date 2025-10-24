package app.domain.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

/**
 * 임시 API 키 모델
 * 테스트 페이지에서 제한된 횟수만큼 사용 가능한 임시 키
 */
@Getter
@Builder
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class TemporaryApiKey {
    private String apiKey;
    private String ipAddress;
    @Builder.Default
    private Integer remainingCount = 10; // 기본 10회 사용 가능
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;

    /**
     * 사용 횟수 감소
     * @return 남은 사용 횟수
     */
    public int decrementUsage() {
        if (remainingCount > 0) {
            remainingCount--;
        }
        return remainingCount;
    }

    /**
     * 사용 가능 여부 확인
     */
    public boolean isValid() {
        return remainingCount > 0 && LocalDateTime.now().isBefore(expiredAt);
    }
}
