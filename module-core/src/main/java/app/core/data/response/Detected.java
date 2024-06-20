package app.core.data.response;

/**
 * 검출된 단어에 대한 정보를 담는 클래스
 */
public record Detected(
        int length,
        String word
) {
}
