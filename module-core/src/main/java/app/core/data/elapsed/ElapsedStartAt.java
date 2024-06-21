package app.core.data.elapsed;

import lombok.Getter;

@Getter
public class ElapsedStartAt {
    private final Long startAt;

    private ElapsedStartAt() {
        this.startAt = System.nanoTime();
    }

    public static ElapsedStartAt now() {
        return new ElapsedStartAt();
    }
}
