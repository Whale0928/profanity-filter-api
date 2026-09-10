package app.application.admin;

import java.time.Instant;

/**
 * 사전 단어가 변경되었음을 알리는 이벤트입니다. 커밋 이후에만 Trie를 재동기화하기 위해 사용합니다.
 *
 * @param wordId 변경된 단어 식별자. 신규 등록 직후에는 null일 수 있습니다.
 * @param changedAt 변경 시각
 */
public record ProfanityDictionaryChangedEvent(Long wordId, Instant changedAt) {}
