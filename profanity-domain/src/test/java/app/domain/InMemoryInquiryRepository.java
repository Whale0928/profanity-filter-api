package app.domain;

import app.domain.inquiry.Inquiry;
import app.domain.inquiry.InquiryRepository;
import app.domain.inquiry.InquiryStatus;
import app.domain.inquiry.InquiryType;
import app.domain.support.PageResult;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryInquiryRepository implements InquiryRepository {

  private final Map<Long, Inquiry> values = new LinkedHashMap<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public Inquiry save(Inquiry inquiry) {
    if (inquiry.getId() == null) {
      assignId(inquiry, sequence.incrementAndGet());
    }
    values.put(inquiry.getId(), inquiry);
    return inquiry;
  }

  @Override
  public Optional<Inquiry> findById(Long id) {
    return Optional.ofNullable(values.get(id));
  }

  @Override
  public Optional<Inquiry> findByIdForUpdate(Long id) {
    return findById(id);
  }

  @Override
  public PageResult<Inquiry> searchForAdmin(
      InquiryType type, InquiryStatus status, String query, int page, int size) {
    List<Inquiry> matched =
        values.values().stream()
            .filter(inquiry -> type == null || inquiry.getType() == type)
            .filter(inquiry -> status == null || inquiry.getStatus() == status)
            .filter(inquiry -> matches(inquiry, query))
            .sorted(Comparator.comparing(Inquiry::getId).reversed())
            .toList();
    return slice(matched, page, size);
  }

  @Override
  public PageResult<Inquiry> searchByRequester(
      UUID requesterUserId, String query, int page, int size) {
    List<Inquiry> matched =
        values.values().stream()
            .filter(inquiry -> inquiry.isOwnedBy(requesterUserId))
            .filter(inquiry -> matches(inquiry, query))
            .sorted(Comparator.comparing(Inquiry::getId).reversed())
            .toList();
    return slice(matched, page, size);
  }

  private static boolean matches(Inquiry inquiry, String query) {
    if (query == null) {
      return true;
    }
    String keyword = query.toLowerCase(Locale.ROOT);
    return inquiry.getTitle().toLowerCase(Locale.ROOT).contains(keyword)
        || inquiry.getContent().toLowerCase(Locale.ROOT).contains(keyword);
  }

  private static PageResult<Inquiry> slice(List<Inquiry> matched, int page, int size) {
    int from = Math.min(page * size, matched.size());
    int to = Math.min(from + size, matched.size());
    return PageResult.of(matched.subList(from, to), page, to < matched.size());
  }

  private static void assignId(Inquiry inquiry, long id) {
    try {
      Field field = Inquiry.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(inquiry, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
