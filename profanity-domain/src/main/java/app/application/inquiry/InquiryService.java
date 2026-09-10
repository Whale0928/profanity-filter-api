package app.application.inquiry;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.inquiry.Inquiry;
import app.domain.inquiry.InquiryReply;
import app.domain.inquiry.InquiryReplyRepository;
import app.domain.inquiry.InquiryRepository;
import app.domain.manage.WordManagementRepository;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 사용자가 자신의 문의를 등록하고 조회하는 기능입니다.
 *
 * <p>CLIENT와 ADMIN 모두 이 경로에서는 본인이 등록한 문의만 다룹니다. 다른 사용자의 문의를 요청하면 관리자라도 접근이 거절됩니다.
 */
@Service
@RequiredArgsConstructor
public class InquiryService {

  private final InquiryRepository inquiryRepository;
  private final InquiryReplyRepository inquiryReplyRepository;
  private final InquiryRequesterResolver requesterResolver;
  private final WordRequestInquiryRegistrar wordRequestRegistrar;
  private final WordManagementRepository wordManagementRepository;
  private final Clock loginAuthClock;

  @Transactional(readOnly = true)
  public PageResult<InquiryView> findMine(UUID userId, String query, PageQuery pageQuery) {
    PageResult<Inquiry> page =
        inquiryRepository.searchByRequester(
            userId, PageQuery.normalizeQuery(query), pageQuery.page(), pageQuery.size());
    Map<Long, InquiryRequester> requesters = requesterResolver.resolveAll(page.items());
    return page.map(
        inquiry ->
            InquiryView.of(
                inquiry, requesters.getOrDefault(inquiry.getId(), InquiryRequester.unknown())));
  }

  /**
   * 본인이 등록한 문의 하나를 조회합니다.
   *
   * @throws BusinessException 문의가 없거나 본인이 등록한 문의가 아닌 경우
   */
  @Transactional(readOnly = true)
  public InquiryDetailView findMineDetail(UUID userId, Long inquiryId) {
    Inquiry inquiry =
        inquiryRepository
            .findById(inquiryId)
            .orElseThrow(() -> new BusinessException(StatusCode.INQUIRY_NOT_FOUND));
    if (!inquiry.isOwnedBy(userId)) {
      throw new BusinessException(StatusCode.INQUIRY_ACCESS_DENIED);
    }
    return detailOf(inquiry);
  }

  /** 새 문의를 등록합니다. 단어 요청이면 같은 트랜잭션에서 단어 요청도 함께 만듭니다. */
  @Transactional
  public InquiryDetailView create(UUID userId, NewInquiryCommand command) {
    Inquiry inquiry =
        inquiryRepository.save(
            Inquiry.fromLoginUser(
                command.type(),
                command.title(),
                command.content(),
                userId,
                loginAuthClock.instant()));
    if (command.isWordRequest()) {
      // 별도 사유 입력이 없는 계약이므로 문의 본문을 단어 요청 사유로 사용합니다.
      wordRequestRegistrar.registerFromLoginUser(
          inquiry, command.word(), command.content(), command.severity(), command.requestType());
    }
    return detailOf(inquiry);
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
