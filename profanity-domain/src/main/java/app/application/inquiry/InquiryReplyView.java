package app.application.inquiry;

import app.domain.inquiry.InquiryReply;
import java.time.Instant;

/** 문의에 달린 답변 항목입니다. */
public record InquiryReplyView(Long id, String content, String authorName, Instant createdAt) {

  public static InquiryReplyView of(InquiryReply reply, String authorName) {
    return new InquiryReplyView(
        reply.getId(), reply.getContent(), authorName, reply.getCreatedAt());
  }
}
