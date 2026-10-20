package app.domain.whitelist;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;

/** 허용 단어 목록을 줄바꿈으로 이어 TEXT 컬럼 하나에 담습니다. 단어에는 줄바꿈을 허용하지 않으므로 구분자와 겹치지 않습니다. */
@Converter
public class WhitelistWordsConverter implements AttributeConverter<List<String>, String> {

  private static final String SEPARATOR = "\n";

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "";
    }
    return String.join(SEPARATOR, attribute);
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return List.of();
    }
    return Arrays.stream(dbData.split(SEPARATOR)).filter(word -> !word.isEmpty()).toList();
  }
}
