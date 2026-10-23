package app.application.filter;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
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
   * {@link #clean}과 같은 규칙으로 정리하면서 남은 글자마다 원문 위치를 함께 기록합니다.
   *
   * <p>매칭은 정리된 문자열에서 일어나지만 응답과 마스킹은 원문 위치를 써야 합니다. 원문에서 글자를 다시 찾으면 앞에 있는 같은 글자를 잡거나 대소문자가 달라 놓치므로,
   * 정리하는 순간의 위치를 그대로 씁니다.
   */
  static Cleaned cleanWithOffsets(String text) {
    StringBuilder cleaned = new StringBuilder(text.length());
    int[] offsets = new int[text.length()];
    int size = 0;
    int from = 0;
    Matcher removed = NOT_MATCHABLE.matcher(text);
    while (removed.find()) {
      for (int i = from; i < removed.start(); i++) {
        cleaned.append(text.charAt(i));
        offsets[size++] = i;
      }
      from = removed.end();
    }
    for (int i = from; i < text.length(); i++) {
      cleaned.append(text.charAt(i));
      offsets[size++] = i;
    }
    return new Cleaned(cleaned.toString(), Arrays.copyOf(offsets, size));
  }

  /** 정리된 문자열과, 그 문자열의 각 글자가 원문에서 차지하던 위치입니다. */
  record Cleaned(String text, int[] offsets) {

    /** 정리된 문자열의 시작 위치를 원문 위치로 바꿉니다. */
    int originalStart(int start) {
      return offsets[start];
    }

    /** 정리된 문자열의 마지막 글자 위치(포함)를 원문의 끝 위치(배타)로 바꿉니다. Aho-Corasick의 Emit 끝 위치가 포함 구간입니다. */
    int originalEnd(int endInclusive) {
      return offsets[endInclusive] + 1;
    }
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
