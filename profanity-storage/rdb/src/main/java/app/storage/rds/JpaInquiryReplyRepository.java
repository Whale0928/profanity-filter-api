package app.storage.rds;

import app.domain.inquiry.InquiryReply;
import app.domain.inquiry.InquiryReplyRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInquiryReplyRepository
    extends InquiryReplyRepository, JpaRepository<InquiryReply, Long> {}
