package app.application.filter;

import java.time.Instant;

/**
 * 적재된 사전 단어 집합이 실제로 바뀐 채로 Trie 재동기화가 끝났음을 알립니다.
 *
 * <p>이전 사전으로 계산한 필터 결과 캐시를 인스턴스마다 비우기 위한 신호입니다.
 *
 * @param reloadedAt 재동기화가 끝난 시각
 */
public record ProfanityDictionaryReloadedEvent(Instant reloadedAt) {}
