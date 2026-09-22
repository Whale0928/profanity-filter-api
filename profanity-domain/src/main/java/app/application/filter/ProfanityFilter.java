package app.application.filter;

import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import java.util.Set;

public interface ProfanityFilter {
  Boolean containsProfanity(String text);

  FilterResponse allMatched(String text);

  /**
   * 허용 단어를 뺀 모든 검출을 돌려줍니다.
   *
   * @param allowedWords {@link ProfanityText#comparisonKey}로 정리한 허용 단어. 비어 있으면 {@link
   *     #allMatched(String)}와 같습니다.
   */
  FilterResponse allMatched(String text, Set<String> allowedWords);

  FilterWord firstMatched(String text);

  /**
   * 허용 단어가 아닌 첫 검출을 돌려줍니다. 첫 검출이 허용 단어이면 그 뒤의 검출을 찾습니다.
   *
   * @param allowedWords {@link ProfanityText#comparisonKey}로 정리한 허용 단어. 비어 있으면 {@link
   *     #firstMatched(String)}와 같습니다.
   */
  FilterWord firstMatched(String text, Set<String> allowedWords);
}
