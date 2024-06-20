package app.core.data.response;

/**
 * 처리 시간을 나타내는 값 입니다.
 */
public record Elapsed(
        long seconds,
        long milliseconds
) {
    @Override
    public String toString() {
        return seconds + " s, " + milliseconds + " ms";
    }
}
