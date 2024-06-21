package app.core.data.elapsed;

import lombok.Getter;
import org.springframework.lang.NonNull;

/**
 * 처리 시간을 나타내는 값 입니다.
 */
@Getter
public class Elapsed {
    private final Long milliseconds;

    public Elapsed(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public static Elapsed end(@NonNull ElapsedStartAt startAt) {
        Long start = startAt.getStartAt();
        Long end = System.nanoTime();

        long totalMilliseconds = (end - start) / 1_000_000; // 나노초에서 밀리초로 변환
        return new Elapsed(totalMilliseconds);
    }

    public Long milliseconds() {
        return milliseconds;
    }

    @Override
    public String toString() {
        return milliseconds + " ms";
    }
}
