package app.core.util;

/** API 키 등 민감 문자열을 로그에 안전하게 남기기 위한 마스킹 유틸. */
public final class ApiKeys {

  private static final String MASK = "****";
  private static final int VISIBLE_PREFIX = 4;

  private ApiKeys() {}

  /** API 키를 앞 {@value #VISIBLE_PREFIX}자만 노출하고 나머지는 마스킹한다. null/blank는 마스킹 문자열만 반환. */
  public static String mask(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      return MASK;
    }
    int visible = Math.min(VISIBLE_PREFIX, apiKey.length());
    return apiKey.substring(0, visible) + MASK;
  }
}
