package app.application.inquiry;

import java.time.Instant;
import java.util.List;

/** 문의 상세입니다. 답변 목록과 연결된 단어 요청을 함께 담습니다. */
public record InquiryDetailView(
    Long id,
    String type,
    String title,
    String content,
    String status,
    String requesterName,
    String requesterEmail,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt,
    List<InquiryReplyView> replies,
    WordRequestView wordRequest) {

  public static InquiryDetailView of(
      InquiryView inquiry, List<InquiryReplyView> replies, WordRequestView wordRequest) {
    return new InquiryDetailView(
        inquiry.id(),
        inquiry.type(),
        inquiry.title(),
        inquiry.content(),
        inquiry.status(),
        inquiry.requesterName(),
        inquiry.requesterEmail(),
        inquiry.createdAt(),
        inquiry.updatedAt(),
        inquiry.resolvedAt(),
        replies == null ? List.of() : List.copyOf(replies),
        wordRequest);
  }
}
