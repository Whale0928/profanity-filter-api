package app.storage.rds;

import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.isUsedType;
import app.domain.support.PageResult;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaProfanityRepository
    extends ProfanityRepository, JpaRepository<ProfanityWord, Long> {

  @Override
  @Query("SELECT COUNT(p) FROM profanity_word p")
  long countAll();

  @Override
  @Query(
      "SELECT COUNT(p) FROM profanity_word p WHERE p.isUsed = app.domain.profanity.constant.isUsedType.Y")
  long countUsedWords();

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from profanity_word p where p.id = :id")
  Optional<ProfanityWord> findByIdForUpdate(@Param("id") Long id);

  /**
   * 사용 여부 파라미터를 JPQL에서 null 비교하지 않도록 분기합니다. AttributeConverter 없이 매핑한 enum이라도 {@code :param is
   * null} 비교는 타입 추론이 불안정하기 때문입니다.
   */
  @Override
  default PageResult<ProfanityWord> searchForAdmin(
      String query, isUsedType isUsed, int page, int size) {
    Pageable pageable = PageResults.request(page, size, Sort.by(Sort.Direction.DESC, "id"));
    Slice<ProfanityWord> slice =
        isUsed == null ? searchSlice(query, pageable) : searchSlice(query, isUsed, pageable);
    return PageResults.from(slice);
  }

  @Query(
      """
      select p from profanity_word p
      where :query is null or lower(p.word) like lower(concat('%', :query, '%'))
      """)
  Slice<ProfanityWord> searchSlice(@Param("query") String query, Pageable pageable);

  @Query(
      """
      select p from profanity_word p
      where p.isUsed = :isUsed
        and (:query is null or lower(p.word) like lower(concat('%', :query, '%')))
      """)
  Slice<ProfanityWord> searchSlice(
      @Param("query") String query, @Param("isUsed") isUsedType isUsed, Pageable pageable);
}
