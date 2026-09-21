package app.domain.profanity;

import app.domain.profanity.constant.isUsedType;
import app.domain.support.PageResult;
import java.util.List;
import java.util.Optional;

public interface ProfanityRepository {

  Optional<ProfanityWord> findById(Long id);

  /** 관리자 수정 경합에서 마지막 쓰기가 이전 변경을 덮어쓰지 않도록 행을 잠그고 조회합니다. */
  Optional<ProfanityWord> findByIdForUpdate(Long id);

  ProfanityWord save(ProfanityWord profanityWord);

  List<ProfanityWord> findAll();

  /** 지정한 사용 여부의 단어만 조회합니다. Trie는 사용 중인 단어만 적재합니다. */
  List<ProfanityWord> findAllByIsUsed(isUsedType isUsed);

  /** 사용 중으로 표시된 사전 단어 수입니다. Trie에 적재되는 단어와 같은 기준입니다. */
  long countUsedWords();

  Optional<ProfanityWord> findByWord(String word);

  void deleteAll();

  long countAll();

  /**
   * 관리자 사전 목록을 조회합니다.
   *
   * @param query 단어에 대한 부분 일치 검색어. null이면 전체입니다.
   * @param isUsed 사용 여부 필터. null이면 전체입니다.
   */
  PageResult<ProfanityWord> searchForAdmin(String query, isUsedType isUsed, int page, int size);
}
