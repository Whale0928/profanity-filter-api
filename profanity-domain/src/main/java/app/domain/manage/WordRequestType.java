package app.domain.manage;

import java.util.Arrays;
import java.util.Locale;

/**
 * 단어 요청 유형입니다. 외부 API가 노출하는 이름은 ADD, REMOVE, MODIFY이지만 저장된 값은 각각 NEW, EXCEPTION, MODIFY입니다.
 *
 * <p>기존 데이터의 값을 그대로 보존해야 하므로 저장 값을 바꾸지 않고 조회 시점에 외부 이름으로 변환합니다.
 */
public enum WordRequestType {
  ADD("NEW"),
  REMOVE("EXCEPTION"),
  MODIFY("MODIFY");

  private final String storedValue;

  WordRequestType(String storedValue) {
    this.storedValue = storedValue;
  }

  public String storedValue() {
    return storedValue;
  }

  /** 저장된 값을 외부 이름으로 변환합니다. 알 수 없는 값이면 비어 있는 결과 대신 null을 반환합니다. */
  public static WordRequestType fromStored(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(type -> type.storedValue.equals(normalized) || type.name().equals(normalized))
        .findFirst()
        .orElse(null);
  }
}
