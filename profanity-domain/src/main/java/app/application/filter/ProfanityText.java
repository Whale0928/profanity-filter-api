package app.application.filter;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 필터가 문자열을 비교할 때 쓰는 정리 규칙입니다.
 *
 * <p>검출은 사전 단어 기준으로 일어나지만 응답에는 원문 조각이 나갑니다. 고객은 응답에서 본 `바-보` 같은 표현을 허용 단어로 등록하므로, 사전 단어와 허용 단어를 같은
 * 규칙으로 정리해야 서로 맞습니다. 규칙을 한 곳에 두어 필터와 허용 단어 비교가 어긋나지 않게 합니다.
 */
public final class ProfanityText {

  private static final Pattern NOT_MATCHABLE = Pattern.compile("[^ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z\\s]");

  private ProfanityText() {}

  /** 매칭 전에 적용하는 정리입니다. 한글, 영문, 공백만 남깁니다. */
  public static String clean(String text) {
    return NOT_MATCHABLE.matcher(text).replaceAll("");
  }

  /**
   * 허용 단어와 사전 단어를 비교하는 키입니다. 필터가 대소문자를 구분하지 않으므로 소문자로 맞춥니다.
   *
   * @return 한글이나 영문이 하나도 없으면 빈 문자열
   */
  public static String comparisonKey(String word) {
    if (word == null) {
      return "";
    }
    return clean(word).trim().toLowerCase(Locale.ROOT);
  }
}
