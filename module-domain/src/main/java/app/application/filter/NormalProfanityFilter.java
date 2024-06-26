package app.application.filter;

import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import jakarta.annotation.PostConstruct;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NormalProfanityFilter implements ProfanityFilter, AhocorasickFilter {

    private static final Logger log = LoggerFactory.getLogger(NormalProfanityFilter.class);
    private final ProfanityRepository profanityRepository;
    private static Trie trie;
    private Set<String> collect = new HashSet<>();

    public NormalProfanityFilter(ProfanityRepository profanityRepository) {
        this.profanityRepository = profanityRepository;
        trie = Trie.builder().build();
    }

    @PostConstruct
    public void postConstruct() {
        synchronizeProfanityTrie();
    }

    @Override
    public void synchronizeProfanityTrie() {
        log.info("[NormalProfanityFilter] 비속어 자료 로딩 시작 : {}", LocalDateTime.now());
        ElapsedStartAt start = ElapsedStartAt.now();

        collect.clear();
        collect = profanityRepository.findAll()
                .stream()
                .map(ProfanityWord::getWord)
                .collect(Collectors.toSet());

        trie = Trie.builder()
                //.ignoreOverlaps()
                .addKeywords(collect)
                .build();

        Elapsed elapsed = Elapsed.end(start);
        log.info("[NormalProfanityFilter] 비속어 자료 로딩 완료 {}개 (지연 시간 : {}ms)", collect.size(), elapsed);
    }

    @Override
    public List<?> getProfanityTrieList() {
        log.info("[NormalProfanityFilter] 비속어 자료 목록 조회 : {}", LocalDateTime.now());
        return collect.stream().toList();
    }

    @Override
    public Boolean containsProfanity(String text) {
        return !trie.parseText(text).isEmpty();
    }

    @Override
    public FilterResponse allMatched(String text) {
        log.info("[NormalProfanityFilter] 전체 비속어 필터링 시작 : {}", LocalDateTime.now());
        ElapsedStartAt start = ElapsedStartAt.now();

        Set<FilterWord> filterWords = trie.parseText(text)
                .stream()
                .map(emit -> FilterWord.create(emit.getKeyword(), emit.getStart(), emit.getEnd()))
                .collect(Collectors.toSet());

        Elapsed elapsed = Elapsed.end(start);
        log.info("전체 비속어 필터링 : {} (지연 시간 : {}ms)", text, elapsed);
        return FilterResponse.create(text, filterWords, elapsed);
    }

    @Override
    public FilterWord firstMatched(String text) {
        log.info("[NormalProfanityFilter] 단일 비속어 필터링 시작 : {}", LocalDateTime.now());
        ElapsedStartAt start = ElapsedStartAt.now();
        Emit emit = trie.firstMatch(text);
        Elapsed elapsed = Elapsed.end(start);

        String keyword = emit.getKeyword();
        int wordStart = emit.getStart();
        int wordEnd = emit.getEnd();

        log.info("단일 비속어 필터링 : {} (지연 시간 : {}ms)", text, elapsed);
        return FilterWord.create(keyword, wordStart, wordEnd);
    }


}
