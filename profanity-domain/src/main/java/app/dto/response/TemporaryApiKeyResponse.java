package app.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 임시 API 키 발급 응답
 */
@Builder
public record TemporaryApiKeyResponse(
        String apiKey,
        Integer remainingCount,
        LocalDateTime issuedAt,
        LocalDateTime expiredAt,
        String message
) {
    public static TemporaryApiKeyResponse of(
            String apiKey,
            Integer remainingCount,
            LocalDateTime issuedAt,
            LocalDateTime expiredAt
    ) {
        return TemporaryApiKeyResponse.builder()
                .apiKey(apiKey)
                .remainingCount(remainingCount)
                .issuedAt(issuedAt)
                .expiredAt(expiredAt)
                .message("임시 API 키가 발급되었습니다. " + remainingCount + "회 사용 가능합니다.")
                .build();
    }
}
