package app.application.manage;

import static app.dto.message.SuccessBusinessMessage.REQUEST_SUCCESS;

import app.application.inquiry.WordRequestInquiryRegistrar;
import app.domain.manage.WordRequestType;
import app.dto.response.MessageResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기존 외부 단어 요청 API의 처리 서비스입니다.
 *
 * <p>입력과 응답 계약은 그대로 두면서, 단어 요청을 만들 때 같은 트랜잭션에서 문의도 함께 생성합니다. requestUserId는 과거와 동일하게 API Key
 * 식별자입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultWordManagementService implements WordManagementService {

  private final WordRequestInquiryRegistrar wordRequestRegistrar;

  @Override
  @Transactional
  public MessageResponse requestNewWord(
      UUID requestUserId, String word, String reason, String severity) {
    return register(requestUserId, word, reason, severity, WordRequestType.ADD);
  }

  @Override
  @Transactional
  public MessageResponse exceptionWord(
      UUID requestUserId, String word, String reason, String severity) {
    return register(requestUserId, word, reason, severity, WordRequestType.REMOVE);
  }

  @Override
  @Transactional
  public MessageResponse modifyWord(
      UUID requestUserId, String word, String reason, String severity) {
    return register(requestUserId, word, reason, severity, WordRequestType.MODIFY);
  }

  private MessageResponse register(
      UUID requestUserId, String word, String reason, String severity, WordRequestType type) {
    wordRequestRegistrar.registerFromApiKey(requestUserId, word, reason, severity, type);
    return MessageResponse.of(REQUEST_SUCCESS);
  }

  @Override
  public boolean acceptWord(List<Long> requestId) {
    return false;
  }
}
