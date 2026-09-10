package app.domain.profanity.constant;

/** 사전 단어가 어떤 경로로 등록되었는지 나타냅니다. V5 이전에 등록된 단어는 출처를 알 수 없어 UNKNOWN입니다. */
public enum WordSource {
  BASE,
  ADMIN,
  REQUEST,
  UNKNOWN;

  public static WordSource defaultSource() {
    return UNKNOWN;
  }
}
