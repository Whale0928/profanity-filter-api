package app.domain;

import app.domain.manage.WordManagementRepository;
import app.domain.manage.WordManagementRequest;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryWordManagementRepository implements WordManagementRepository {

  private final Map<Long, WordManagementRequest> values = new LinkedHashMap<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public Optional<WordManagementRequest> findById(Long id) {
    return Optional.ofNullable(values.get(id));
  }

  @Override
  public WordManagementRequest save(WordManagementRequest request) {
    if (request.getId() == null) {
      assignId(request, sequence.incrementAndGet());
    }
    values.put(request.getId(), request);
    return request;
  }

  @Override
  public List<WordManagementRequest> findAll() {
    return new ArrayList<>(values.values());
  }

  @Override
  public Optional<WordManagementRequest> findByInquiryId(Long inquiryId) {
    return values.values().stream()
        .filter(request -> Objects.equals(request.getInquiryId(), inquiryId))
        .findFirst();
  }

  @Override
  public Optional<WordManagementRequest> findByInquiryIdForUpdate(Long inquiryId) {
    return findByInquiryId(inquiryId);
  }

  @Override
  public List<WordManagementRequest> findAllByInquiryIdIn(Collection<Long> inquiryIds) {
    return values.values().stream()
        .filter(request -> inquiryIds.contains(request.getInquiryId()))
        .toList();
  }

  @Override
  public Boolean activateWord(Long id) {
    return findById(id).isPresent();
  }

  private static void assignId(WordManagementRequest request, long id) {
    try {
      Field field = WordManagementRequest.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(request, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
