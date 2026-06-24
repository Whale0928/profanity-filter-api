package app.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiKeysTest {

  @Nested
  @DisplayName("정상 키를 마스킹할 때")
  class ValidKey {

    @Test
    @DisplayName("앞 4자만 노출하고 나머지는 마스킹한다")
    void masksAllButPrefix() {
      assertThat(ApiKeys.mask("1F8LlsAIZeKSCMlZoBwoSoeRIg6Q5D5N68zepccT0jY")).isEqualTo("1F8L****");
    }

    @Test
    @DisplayName("정확히 4자면 전체가 노출되지만 마스킹 접미가 붙는다")
    void exactlyFourChars() {
      assertThat(ApiKeys.mask("abcd")).isEqualTo("abcd****");
    }

    @Test
    @DisplayName("4자 미만이면 있는 만큼만 노출한다")
    void shorterThanPrefix() {
      assertThat(ApiKeys.mask("ab")).isEqualTo("ab****");
    }
  }

  @Nested
  @DisplayName("비어 있거나 null인 키일 때")
  class BlankKey {

    @Test
    @DisplayName("null이면 마스킹 문자열만 반환한다")
    void nullReturnsMask() {
      assertThat(ApiKeys.mask(null)).isEqualTo("****");
    }

    @Test
    @DisplayName("빈 문자열이면 마스킹 문자열만 반환한다")
    void emptyReturnsMask() {
      assertThat(ApiKeys.mask("")).isEqualTo("****");
    }

    @Test
    @DisplayName("공백만 있으면 마스킹 문자열만 반환한다")
    void whitespaceReturnsMask() {
      assertThat(ApiKeys.mask("   ")).isEqualTo("****");
    }
  }
}
