package app.domain;

import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.isUsedType;
import app.domain.support.PageResult;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InmemoryProfanityRepository implements ProfanityRepository {

  private final Map<Long, ProfanityWord> repository = new LinkedHashMap<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public Optional<ProfanityWord> findById(Long id) {
    return Optional.ofNullable(repository.get(id));
  }

  @Override
  public Optional<ProfanityWord> findByIdForUpdate(Long id) {
    return findById(id);
  }

  @Override
  public ProfanityWord save(ProfanityWord profanityWord) {
    if (profanityWord.getId() == null) {
      assignId(profanityWord, sequence.incrementAndGet());
    }
    repository.put(profanityWord.getId(), profanityWord);
    return profanityWord;
  }

  @Override
  public List<ProfanityWord> findAll() {
    return new ArrayList<>(repository.values());
  }

  @Override
  public List<ProfanityWord> findAllByIsUsed(isUsedType isUsed) {
    return repository.values().stream().filter(word -> word.getIsUsed() == isUsed).toList();
  }

  @Override
  public Optional<ProfanityWord> findByWord(String word) {
    return repository.values().stream().filter(saved -> saved.getWord().equals(word)).findFirst();
  }

  @Override
  public void deleteAll() {
    repository.clear();
  }

  @Override
  public long countAll() {
    return repository.size();
  }

  @Override
  public PageResult<ProfanityWord> searchForAdmin(
      String query, isUsedType isUsed, int page, int size) {
    List<ProfanityWord> matched =
        repository.values().stream()
            .filter(word -> query == null || containsIgnoreCase(word.getWord(), query))
            .filter(word -> isUsed == null || word.getIsUsed() == isUsed)
            .sorted(Comparator.comparing(ProfanityWord::getId).reversed())
            .toList();
    int from = Math.min(page * size, matched.size());
    int to = Math.min(from + size, matched.size());
    return PageResult.of(matched.subList(from, to), page, to < matched.size());
  }

  private static boolean containsIgnoreCase(String value, String query) {
    return value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
  }

  private static void assignId(ProfanityWord word, long id) {
    try {
      Field field = ProfanityWord.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(word, id);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
