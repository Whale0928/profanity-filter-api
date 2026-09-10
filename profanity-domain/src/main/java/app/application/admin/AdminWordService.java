package app.application.admin;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.WordSource;
import app.domain.profanity.constant.isUsedType;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 필터 사전 관리 기능입니다.
 *
 * <p>사전을 바꾸는 모든 경로는 {@link ProfanityDictionaryChangedEvent}를 발행하며, 커밋 이후에 Trie가 재동기화됩니다. 단어 요청 승인
 * 경로는 AdminInquiryService에서 처리하며 같은 커밋 후 동기화 이벤트를 사용합니다.
 */
@Service
@RequiredArgsConstructor
public class AdminWordService {

  private final ProfanityRepository profanityRepository;
  private final AdminAuditService adminAuditService;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock loginAuthClock;

  @Transactional(readOnly = true)
  public PageResult<AdminWordView> search(String query, isUsedType isUsed, PageQuery pageQuery) {
    return profanityRepository
        .searchForAdmin(PageQuery.normalizeQuery(query), isUsed, pageQuery.page(), pageQuery.size())
        .map(AdminWordView::from);
  }

  /**
   * 관리자가 사전 단어를 등록합니다.
   *
   * @throws BusinessException 이미 같은 단어가 있는 경우
   */
  @Transactional
  public AdminWordView create(UUID actorId, String word) {
    String normalized = requireWord(word);
    requireAbsent(normalized, null);

    Instant now = loginAuthClock.instant();
    ProfanityWord saved =
        profanityRepository.save(ProfanityWord.create(normalized, WordSource.ADMIN, actorId, now));
    adminAuditService.record(
        actorId,
        AdminAuditAction.WORD_CREATED,
        AdminAuditTargetType.WORD,
        String.valueOf(saved.getId()),
        normalized,
        now);
    publishDictionaryChanged(saved.getId(), now);
    return AdminWordView.from(saved);
  }

  /**
   * 관리자가 사전 단어의 표현과 사용 여부를 수정합니다. 동시 수정으로 이전 변경이 사라지지 않도록 대상 행을 잠그고 진행합니다.
   *
   * @throws BusinessException 대상이 없거나 다른 단어와 표현이 겹치는 경우
   */
  @Transactional
  public AdminWordView update(UUID actorId, Long wordId, String word, isUsedType isUsed) {
    ProfanityWord target =
        profanityRepository
            .findByIdForUpdate(wordId)
            .orElseThrow(() -> new BusinessException(StatusCode.WORD_NOT_FOUND));
    String normalized = requireWord(word);
    requireAbsent(normalized, wordId);

    Instant now = loginAuthClock.instant();
    target.rename(normalized, actorId, now);
    target.changeUsage(isUsed, actorId, now);
    ProfanityWord saved = profanityRepository.save(target);
    adminAuditService.record(
        actorId,
        AdminAuditAction.WORD_UPDATED,
        AdminAuditTargetType.WORD,
        String.valueOf(wordId),
        normalized + "/" + isUsed.name(),
        now);
    publishDictionaryChanged(wordId, now);
    return AdminWordView.from(saved);
  }

  private void requireAbsent(String normalized, Long allowedId) {
    profanityRepository
        .findByWord(normalized)
        .filter(existing -> allowedId == null || !allowedId.equals(existing.getId()))
        .ifPresent(
            existing -> {
              throw new BusinessException(StatusCode.WORD_ALREADY_EXISTS);
            });
  }

  private void publishDictionaryChanged(Long wordId, Instant now) {
    eventPublisher.publishEvent(new ProfanityDictionaryChangedEvent(wordId, now));
  }

  private static String requireWord(String value) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(StatusCode.BAD_REQUEST, "word는 공백일 수 없습니다.");
    }
    return value.trim();
  }

  /**
   * 관리자 사전 항목입니다.
   *
   * @param createdAt 등록 시각. V5 이전 데이터는 null입니다.
   * @param updatedAt 수정 시각. V5 이전 데이터는 null입니다.
   */
  public record AdminWordView(
      Long id, String word, String isUsed, String source, Instant createdAt, Instant updatedAt) {

    public static AdminWordView from(ProfanityWord word) {
      return new AdminWordView(
          word.getId(),
          word.getWord(),
          word.getIsUsed().name(),
          word.getSource().name(),
          word.getCreatedAt(),
          word.getUpdatedAt());
    }
  }
}
