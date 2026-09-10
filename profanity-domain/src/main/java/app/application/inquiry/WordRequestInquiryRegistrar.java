package app.application.inquiry;

import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import app.domain.inquiry.Inquiry;
import app.domain.inquiry.InquiryRepository;
import app.domain.inquiry.InquiryType;
import app.domain.manage.WordManagementRepository;
import app.domain.manage.WordManagementRequest;
import app.domain.manage.WordRequestType;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 단어 요청을 문의와 함께 등록합니다.
 *
 * <p>기존 외부 API와 대시보드 두 경로가 같은 관계를 만들도록 한곳에 모았습니다. 호출자가 연 트랜잭션 안에서 실행되어야 문의와 단어 요청이 함께 저장됩니다.
 */
@Component
@RequiredArgsConstructor
public class WordRequestInquiryRegistrar {

  private static final int MAX_TITLE_LENGTH = 160;

  private final InquiryRepository inquiryRepository;
  private final WordManagementRepository wordManagementRepository;
  private final ApiKeyRepository apiKeyRepository;
  private final Clock loginAuthClock;

  /**
   * 기존 외부 API로 들어온 단어 요청을 등록합니다.
   *
   * <p>requestUserId는 과거 데이터와 마찬가지로 API Key 식별자입니다. 사용자 계정과 직접 연결하지 않고, 소유자가 확인되는 경우에만 문의에 사용자 식별자를
   * 함께 남깁니다.
   */
  public WordManagementRequest registerFromApiKey(
      UUID apiKeyId, String word, String reason, String severity, WordRequestType requestType) {
    UUID ownerUserId = apiKeyRepository.findById(apiKeyId).map(ApiKey::getUserId).orElse(null);
    Inquiry inquiry =
        inquiryRepository.save(
            Inquiry.fromApiKey(
                InquiryType.WORD_REQUEST,
                title(word),
                reason,
                ownerUserId,
                apiKeyId,
                loginAuthClock.instant()));
    return saveWordRequest(apiKeyId, inquiry.getId(), word, reason, severity, requestType);
  }

  /** 로그인 사용자가 대시보드에서 등록한 단어 요청을 이미 만들어진 문의에 연결합니다. */
  public WordManagementRequest registerFromLoginUser(
      Inquiry inquiry, String word, String reason, String severity, WordRequestType requestType) {
    return saveWordRequest(null, inquiry.getId(), word, reason, severity, requestType);
  }

  /** 문의 제목은 단어를 알아볼 수 있도록 만들고 저장 한계인 160자에서 자릅니다. */
  public static String title(String word) {
    String title = "단어 요청: " + word;
    return title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title;
  }

  private WordManagementRequest saveWordRequest(
      UUID apiKeyId,
      Long inquiryId,
      String word,
      String reason,
      String severity,
      WordRequestType requestType) {
    return wordManagementRepository.save(
        WordManagementRequest.builder()
            .requestUserId(apiKeyId)
            .inquiryId(inquiryId)
            .word(word)
            .reason(reason)
            .severity(severity)
            .requestType(requestType.storedValue())
            .build());
  }
}
