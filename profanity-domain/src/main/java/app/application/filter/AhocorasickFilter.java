package app.application.filter;

import java.util.List;

public interface AhocorasickFilter {
  /**
   * 사전을 다시 읽어 Trie를 재구성합니다.
   *
   * @return 적재된 단어 집합이 실제로 바뀌었으면 true. 호출자는 이 값으로 결과 캐시 무효화 여부를 결정합니다.
   */
  boolean synchronizeProfanityTrie();

  List<?> getProfanityTrieList();
}
