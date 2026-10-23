package app.application.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProfanityTextTest {

  @Nested
  @DisplayName("위치를 기록하는 정리는")
  class CleanWithOffsets {

    @ParameterizedTest
    @DisplayName("clean과 같은 규칙으로 정리한다")
    @ValueSource(strings = {"", "   ", "바-보", "욕12#설", "😀욕설😀", "Fo!oL", "ㅅ ㅂ", "탭\t줄\n바꿈"})
    void cleanWithOffsets_anyText_matchesClean(String text) {
      // when
      ProfanityText.Cleaned cleaned = ProfanityText.cleanWithOffsets(text);

      // then
      assertEquals(ProfanityText.clean(text), cleaned.text());
    }

    @ParameterizedTest
    @DisplayName("정리된 글자마다 원문의 같은 글자 위치를 가리킨다")
    @ValueSource(strings = {"바-보", "욕12#설", "😀욕설😀", "Fo!oL"})
    void cleanWithOffsets_anyText_offsetsPointToSameChar(String text) {
      // when
      ProfanityText.Cleaned cleaned = ProfanityText.cleanWithOffsets(text);

      // then
      for (int i = 0; i < cleaned.text().length(); i++) {
        assertEquals(cleaned.text().charAt(i), text.charAt(cleaned.offsets()[i]));
      }
    }

    @Test
    @DisplayName("정리된 구간을 사이에 끼운 기호까지 포함한 원문 구간으로 바꾼다")
    void originalRange_symbolsInside_coversOriginalFragment() {
      // given
      String text = "이런 바-보야";
      ProfanityText.Cleaned cleaned = ProfanityText.cleanWithOffsets(text);
      int start = cleaned.text().indexOf("바보");

      // when
      int originalStart = cleaned.originalStart(start);
      int originalEnd = cleaned.originalEnd(start + 1);

      // then
      assertEquals("바-보", text.substring(originalStart, originalEnd));
    }
  }
}
