package app.core.data.elapsed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ElapsedTest {

    @Test
    @DisplayName("처리 시간을 나타내는 값이 정상적으로 생성되어야 한다.")
    void test_end() throws Exception {
        // given
        ElapsedStartAt startAt = ElapsedStartAt.now();
        ElapsedStartAt startAt2 = ElapsedStartAt.now();

        // when
        Thread.sleep(100); // 1초 대기
        Elapsed elapsed = Elapsed.end(startAt);
        Thread.sleep(100); // 1초 대기
        Elapsed elapsed2 = Elapsed.end(startAt2);

        // then
        System.out.println(elapsed);
        System.out.println(elapsed2);
        assertNotNull(elapsed);
        assertNotNull(elapsed2);
    }

}
