package app.core.data;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * 두 가지 값을 담는 불변(immutable) 쌍(Pair) 클래스.
 * JPQL에서 직접 인스턴스화 할 수 있도록 public 생성자를 제공합니다.
 *
 * @param <F> 첫 번째 값의 타입
 * @param <S> 두 번째 값의 타입
 */
public class Pair<F, S> {

    private final F first;
    private final S second;

    /**
     * JPQL의 new 연산자에서 사용하기 위한 public 생성자
     */
    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * 정적 팩토리 메소드. 새로운 Pair 인스턴스를 생성합니다.
     */
    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }

    /**
     * Pair 리스트를 Map으로 변환하기 위한 collector입니다.
     */
    public static <F, S> Collector<Pair<F, S>, ?, Map<F, S>> toMap() {
        return Collectors.toMap(Pair::getFirst, Pair::getSecond);
    }

    /**
     * 첫 번째 값을 반환합니다.
     */
    public F getFirst() {
        return first;
    }

    /**
     * 두 번째 값을 반환합니다.
     */
    public S getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) &&
                Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return first + " -> " + second;
    }
}
