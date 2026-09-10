package app.domain.inquiry;

import java.util.Collection;
import java.util.List;

public interface InquiryReplyRepository {
  InquiryReply save(InquiryReply inquiryReply);

  List<InquiryReply> findAllByInquiryIdOrderByIdAsc(Long inquiryId);

  List<InquiryReply> findAllByInquiryIdInOrderByIdAsc(Collection<Long> inquiryIds);
}
