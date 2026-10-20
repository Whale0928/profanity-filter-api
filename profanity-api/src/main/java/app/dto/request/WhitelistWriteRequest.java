package app.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 허용 단어 그룹 생성과 수정 요청입니다.
 *
 * <p>이름과 단어의 세부 규칙은 도메인이 검증합니다. 여기서는 지나치게 큰 입력만 먼저 막습니다. 단어 수는 중복을 뺀 뒤에 세므로 도메인 상한보다 넉넉하게 둡니다.
 */
public record WhitelistWriteRequest(
    @Size(max = 200) String name, @Size(max = 1000) List<@Size(max = 200) String> words) {}
