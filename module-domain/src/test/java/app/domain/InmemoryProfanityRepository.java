package app.domain;

import app.domain.constant.isUsedType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InmemoryProfanityRepository implements ProfanityRepository {

    private final Map<Long, ProfanityWord> repository = new HashMap<>();

    @Override
    public Optional<ProfanityWord> findById(Long id) {
        return Optional.ofNullable(repository.get(id));
    }

    @Override
    public ProfanityWord save(ProfanityWord profanityWord) {
        ProfanityWord word = new ProfanityWord(repository.size() + 1L, profanityWord.getWord(), isUsedType.Y);
        return repository.put(word.getId(), word);
    }

    @Override
    public List<ProfanityWord> findAll() {
        return repository.values().stream().toList();
    }

    @Override
    public void deleteAll() {
        repository.clear();
    }
}
