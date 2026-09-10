package app.domain;

import app.domain.inquiry.InquiryReply;
import app.domain.inquiry.InquiryReplyRepository;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryInquiryReplyRepository implements InquiryReplyRepository {

  private final List<InquiryReply> values = new ArrayList<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public InquiryReply save(InquiryReply inquiryReply) {
    if (inquiryReply.getId() == null) {
      assignId(inquiryReply, sequence.incrementAndGet());
    }
    values.add(inquiryReply);
    return inquiryReply;
  }

  @Override
  public List<InquiryReply> findAllByInquiryIdOrderByIdAsc(Long inquiryId) {
    return values.stream()
        .filter(reply -> reply.getInquiryId().equals(inquiryId))
        .sorted(Comparator.comparing(InquiryReply::getId))
        .toList();
  }

  @Override
  public List<InquiryReply> findAllByInquiryIdInOrderByIdAsc(Collection<Long> inquiryIds) {
    return values.stream()
        .filter(reply -> inquiryIds.contains(reply.getInquiryId()))
        .sorted(Comparator.comparing(InquiryReply::getId))
        .toList();
  }

  private static void assignId(InquiryReply reply, long id) {
    try {
      Field field = InquiryReply.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(reply, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
