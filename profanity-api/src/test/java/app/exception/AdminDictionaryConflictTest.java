package app.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class AdminDictionaryConflictTest {
  @Test
  @DisplayName("동시 사전 등록의 고유 제약 충돌은 409와 중복 단어 코드로 응답한다")
  void duplicateDictionaryWord_returnsConflict() {
    var exception =
        new DataIntegrityViolationException(
            "test duplicate",
            new SQLIntegrityConstraintViolationException(
                "Duplicate entry for key 'profanity_word.uk_profanity_word_word'", "23000", 1062));
    var response = new GlobalExceptionHandler().handleDataIntegrityException(exception);
    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody().status().code()).isEqualTo(4064);
  }

  @Test
  @DisplayName("다른 테이블의 제약 오류를 단어 중복으로 오인하지 않는다")
  void unrelatedConstraint_keepsExistingErrorHandling() {
    var exception =
        new DataIntegrityViolationException(
            "test other constraint",
            new SQLIntegrityConstraintViolationException(
                "Duplicate entry for key 'users.uk_other'", "23000", 1062));
    var response = new GlobalExceptionHandler().handleDataIntegrityException(exception);
    assertThat(response.getBody().status().code()).isNotEqualTo(4064);
  }
}
