package app.application.inquiry;

import app.domain.inquiry.Inquiry;
import java.time.Instant;

/** 문의 목록 항목입니다. */
public record InquiryView(
    Long id,
    String type,
    String title,
    String content,
    String status,
    String requesterName,
    String requesterEmail,
    Instant createdAt,
    Instant updatedAt,
    Instant resolvedAt) {

  public static InquiryView of(Inquiry inquiry, InquiryRequester requester) {
    return new InquiryView(
        inquiry.getId(),
        inquiry.getType().name(),
        inquiry.getTitle(),
        inquiry.getContent(),
        inquiry.getStatus().name(),
        requester.name(),
        requester.email(),
        inquiry.getCreatedAt(),
        inquiry.getUpdatedAt(),
        inquiry.getResolvedAt());
  }
}
