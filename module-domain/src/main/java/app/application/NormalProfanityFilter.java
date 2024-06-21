package app.application;

import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.domain.ProfanityRepository;
import app.domain.ProfanityWord;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import jakarta.annotation.PostConstruct;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NormalProfanityFilter {

    private static final Logger log = LoggerFactory.getLogger(NormalProfanityFilter.class);
    private static Trie trie;
    private final ProfanityRepository profanityRepository;

    public NormalProfanityFilter(ProfanityRepository profanityRepository) {
        this.profanityRepository = profanityRepository;
    }

    @PostConstruct
    public void postConstruct() {
        synchronizeProfanityTrie();
    }

    public void synchronizeProfanityTrie() {
        log.info("비속어 자료 로딩 시작 : {}", LocalDateTime.now());
        ElapsedStartAt start = ElapsedStartAt.now();

        Set<String> collect = profanityRepository.findAll()
                .stream()
                .map(ProfanityWord::getWord)
                .collect(Collectors.toSet());

        trie = Trie.builder()
                .ignoreOverlaps()
                .addKeywords(collect)
                .build();

        Elapsed elapsed = Elapsed.end(start);
        log.info("비속어 자료 로딩 완료 {}개 (지연 시간 : {}ms)", collect.size(), elapsed);
    }

    public FilterResponse allMatched(String word) {
        ElapsedStartAt start = ElapsedStartAt.now();

        List<FilterWord> list = trie.parseText(word)
                .stream()
                .map(emit -> FilterWord.create(emit.getKeyword(), emit.getStart(), emit.getEnd()))
                .toList();
        Elapsed elapsed = Elapsed.end(start);
        log.info("전체 비속어 필터링 : {} (지연 시간 : {}ms)", word, elapsed);
        return FilterResponse.create(word, list, elapsed);
    }

    public FilterWord firstMatched(String word) {

        ElapsedStartAt start = ElapsedStartAt.now();
        Emit emit = trie.firstMatch(word);
        Elapsed elapsed = Elapsed.end(start);

        String keyword = emit.getKeyword();
        int wordStart = emit.getStart();
        int wordEnd = emit.getEnd();


        log.info("단일 비속어 필터링 : {} (지연 시간 : {}ms)", word, elapsed);
        return FilterWord.create(keyword, wordStart, wordEnd);
    }

    public List<?> getProfanityList() {
        return profanityRepository.findAll();
    }
}
