package app.application.inquiry;

import app.application.admin.AdminAuditAction;
import app.application.admin.AdminAuditService;
import app.application.admin.AdminAuditTargetType;
import app.application.admin.ProfanityDictionaryChangedEvent;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.inquiry.Inquiry;
import app.domain.inquiry.InquiryReply;
import app.domain.inquiry.InquiryReplyRepository;
import app.domain.inquiry.InquiryRepository;
import app.domain.inquiry.InquiryStatus;
import app.domain.inquiry.InquiryType;
import app.domain.manage.WordManagementRepository;
import app.domain.manage.WordManagementRequest;
import app.domain.manage.WordRequestType;
import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.WordSource;
import app.domain.profanity.constant.isUsedType;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 문의 처리 기능입니다.
 *
 * <p>답변과 상태 변경은 사전을 건드리지 않습니다. 사전은 단어 요청을 승인할 때만 바뀌며, 같은 요청에 승인이나 거절이 이미 적용되었으면 다시 적용하지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class AdminInquiryService {

  private final InquiryRepository inquiryRepository;
  private final InquiryReplyRepository inquiryReplyRepository;
  private final WordManagementRepository wordManagementRepository;
  private final ProfanityRepository profanityRepository;
  private final InquiryRequesterResolver requesterResolver;
  private final AdminAuditService adminAuditService;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock loginAuthClock;

  @Transactional(readOnly = true)
  public PageResult<InquiryView> search(
      String type, String status, String query, PageQuery pageQuery) {
    PageResult<Inquiry> page =
        inquiryRepository.searchForAdmin(
            InquiryType.parseFilter(type),
            InquiryStatus.parseFilter(status),
            PageQuery.normalizeQuery(query),
            pageQuery.page(),
            pageQuery.size());
    Map<Long, InquiryRequester> requesters = requesterResolver.resolveAll(page.items());
    return page.map(
        inquiry ->
            InquiryView.of(
                inquiry, requesters.getOrDefault(inquiry.getId(), InquiryRequester.unknown())));
  }

  @Transactional(readOnly = true)
  public InquiryDetailView findDetail(Long inquiryId) {
    return detailOf(requireInquiry(inquiryId));
  }

  /** 문의 상태만 변경합니다. 사전과 단어 요청 상태는 그대로 둡니다. */
  @Transactional
  public InquiryDetailView changeStatus(UUID actorId, Long inquiryId, InquiryStatus status) {
    Inquiry inquiry = requireInquiryForUpdate(inquiryId);
    Instant now = loginAuthClock.instant();
    inquiry.changeStatus(status, now);
    Inquiry saved = inquiryRepository.save(inquiry);
    adminAuditService.record(
        actorId,
        AdminAuditAction.INQUIRY_STATUS_CHANGED,
        AdminAuditTargetType.INQUIRY,
        String.valueOf(inquiryId),
        status.name(),
        now);
    return detailOf(saved);
  }

  /**
   * 문의에 답변을 남깁니다.
   *
   * <p>resolve가 true이면 문의를 완료로 옮깁니다. 단어 요청이 연결되어 있어도 답변 완료만으로는 사전이 바뀌지 않습니다.
   */
  @Transactional
  public InquiryDetailView reply(UUID actorId, Long inquiryId, String content, boolean resolve) {
    Inquiry inquiry = requireInquiryForUpdate(inquiryId);
    Instant now = loginAuthClock.instant();
    inquiryReplyRepository.save(InquiryReply.write(inquiryId, actorId, content, now));
    inquiry.markReplied(actorId, now);
    if (resolve) {
      inquiry.changeStatus(InquiryStatus.RESOLVED, now);
    }
    Inquiry saved = inquiryRepository.save(inquiry);
    adminAuditService.record(
        actorId,
        AdminAuditAction.INQUIRY_REPLIED,
        AdminAuditTargetType.INQUIRY,
        String.valueOf(inquiryId),
        saved.getStatus().name(),
        now);
    return detailOf(saved);
  }

  /**
   * 문의에 연결된 단어 요청을 승인하거나 거절합니다.
   *
   * <p>승인은 사전을 바꾸고 거절은 바꾸지 않습니다. 어느 쪽이든 이미 처리된 요청에는 다시 적용되지 않습니다. 문의 상태는 여기서 바꾸지 않으며 답변이나 상태 변경으로
   * 따로 처리합니다.
   *
   * @throws BusinessException 문의나 단어 요청이 없거나, 이미 처리되었거나, 사전에 반영할 수 없는 요청인 경우
   */
  @Transactional
  public InquiryDetailView decideWordRequest(
      UUID actorId, Long inquiryId, WordDecision decision, String reason) {
    Inquiry inquiry = requireInquiryForUpdate(inquiryId);
    WordManagementRequest wordRequest =
        wordManagementRepository
            .findByInquiryIdForUpdate(inquiryId)
            .orElseThrow(() -> new BusinessException(StatusCode.WORD_REQUEST_NOT_FOUND));
    if (!wordRequest.isPending()) {
      throw new BusinessException(StatusCode.WORD_DECISION_ALREADY_APPLIED);
    }

    Instant now = loginAuthClock.instant();
    if (decision == WordDecision.APPROVE) {
      applyToDictionary(actorId, wordRequest, now);
      wordRequest.approve();
    } else {
      wordRequest.reject();
    }
    wordManagementRepository.save(wordRequest);
    adminAuditService.record(
        actorId,
        decision == WordDecision.APPROVE
            ? AdminAuditAction.WORD_REQUEST_APPROVED
            : AdminAuditAction.WORD_REQUEST_REJECTED,
        AdminAuditTargetType.INQUIRY,
        String.valueOf(inquiryId),
        reason,
        now);
    return detailOf(inquiry);
  }

  /**
   * 승인된 요청을 사전에 반영합니다.
   *
   * <p>추가 요청은 없던 단어를 등록하고 제외되어 있던 단어는 다시 사용합니다. 제외 요청은 사전에 있는 단어만 사용 중지로 바꿉니다. 수정 요청은 바꿀 표현을 담는 입력이
   * 없으므로 임의로 추정하지 않고 오류로 처리합니다.
   */
  private void applyToDictionary(UUID actorId, WordManagementRequest wordRequest, Instant now) {
    WordRequestType requestType = wordRequest.exposedRequestType();
    if (requestType == null || requestType == WordRequestType.MODIFY) {
      throw new BusinessException(StatusCode.WORD_MODIFY_TARGET_REQUIRED);
    }
    String word = wordRequest.getWord();
    Optional<ProfanityWord> existing = profanityRepository.findByWord(word);

    if (requestType == WordRequestType.ADD) {
      ProfanityWord saved =
          existing
              .map(found -> activate(found, actorId, now))
              .orElseGet(
                  () ->
                      profanityRepository.save(
                          ProfanityWord.create(word, WordSource.REQUEST, actorId, now)));
      publishDictionaryChanged(saved, now);
      return;
    }

    ProfanityWord target =
        existing.orElseThrow(() -> new BusinessException(StatusCode.WORD_NOT_IN_DICTIONARY));
    target.changeUsage(isUsedType.N, actorId, now);
    publishDictionaryChanged(profanityRepository.save(target), now);
  }

  private ProfanityWord activate(ProfanityWord word, UUID actorId, Instant now) {
    if (word.isUsed()) {
      return word;
    }
    word.changeUsage(isUsedType.Y, actorId, now);
    return profanityRepository.save(word);
  }

  private void publishDictionaryChanged(ProfanityWord word, Instant now) {
    eventPublisher.publishEvent(new ProfanityDictionaryChangedEvent(word.getId(), now));
  }

  private Inquiry requireInquiry(Long inquiryId) {
    return inquiryRepository
        .findById(inquiryId)
        .orElseThrow(() -> new BusinessException(StatusCode.INQUIRY_NOT_FOUND));
  }

  private Inquiry requireInquiryForUpdate(Long inquiryId) {
    return inquiryRepository
        .findByIdForUpdate(inquiryId)
        .orElseThrow(() -> new BusinessException(StatusCode.INQUIRY_NOT_FOUND));
  }

  private InquiryDetailView detailOf(Inquiry inquiry) {
    List<InquiryReply> replies =
        inquiryReplyRepository.findAllByInquiryIdOrderByIdAsc(inquiry.getId());
    Map<UUID, String> authorNames =
        requesterResolver.resolveAuthorNames(
            replies.stream().map(InquiryReply::getAuthorUserId).toList());
    return InquiryDetailView.of(
        InquiryView.of(inquiry, requesterResolver.resolve(inquiry)),
        replies.stream()
            .map(reply -> InquiryReplyView.of(reply, authorNames.get(reply.getAuthorUserId())))
            .toList(),
        wordManagementRepository
            .findByInquiryId(inquiry.getId())
            .map(WordRequestView::from)
            .orElse(null));
  }
}
