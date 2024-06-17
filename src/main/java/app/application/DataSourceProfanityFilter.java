package app.application;

import app.domain.ProfanityWord;
import app.infra.JpaProfanityRepository;
import jakarta.annotation.PostConstruct;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataSourceProfanityFilter {

    private static final Logger log = LoggerFactory.getLogger(DataSourceProfanityFilter.class);
    private final JpaProfanityRepository profanityRepository;
    private static Trie trie;

    public DataSourceProfanityFilter(JpaProfanityRepository profanityRepository) {
        this.profanityRepository = profanityRepository;
    }

    @PostConstruct
    public void load() {
        long startTime = System.nanoTime();

        Set<String> collect = profanityRepository.findAll().stream()
                .map(ProfanityWord::getWord)
                .collect(Collectors.toSet());

        trie = Trie.builder()
                .addKeywords(collect)
                .build();

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;  // convert to milliseconds

        log.info("비속어 사전 로딩 완료 {}개 (지연 시간 : {}ms)", collect.size(), duration);
    }

    public Collection<Emit> containsProfanity(String word) {
        return trie.parseText(word);
    }
}
