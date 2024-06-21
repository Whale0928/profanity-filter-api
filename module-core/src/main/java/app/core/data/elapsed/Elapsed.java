package app.core.data.elapsed;

import lombok.Getter;
import org.springframework.lang.NonNull;

/**
 * 처리 시간을 나타내는 값 입니다.
 */
@Getter
public class Elapsed {
    private final Double milliseconds;
    private final Double microseconds;
    private final Double seconds;

    protected Elapsed(double milliseconds, double microseconds, double seconds) {
        this.milliseconds = milliseconds;
        this.microseconds = microseconds;
        this.seconds = seconds;
    }

    public static Elapsed end(@NonNull ElapsedStartAt startAt) {
        Long start = startAt.getStartAt();
        Long end = System.nanoTime();

        long totalNanoseconds = end - start;
        double totalMilliseconds = totalNanoseconds / 1_000_000.0; // 나노초 => 밀리초
        double totalMicroseconds = totalNanoseconds / 1_000.0; // 나노초 => 마이크로초
        double totalSeconds = totalNanoseconds / 1_000_000_000.0; // 나노초 => 초

        return new Elapsed(totalMilliseconds, totalMicroseconds, totalSeconds);
    }

    @Override
    public String toString() {
        return String.format("%.8f s / %.5f ms / %.3f µs", seconds, milliseconds, microseconds);
    }
}
