package app.application.filter;

import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class NormalProfanityFilter implements ProfanityFilter, AhocorasickFilter {

    private static final Logger log = LoggerFactory.getLogger(NormalProfanityFilter.class);
    private static Trie trie;
    private final ProfanityRepository profanityRepository;
    private Set<String> collect = new HashSet<>();

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
                .ignoreOverlaps()
                .ignoreCase()
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
        
        if (text == null || text.isBlank())
            return FilterResponse.create(text, new HashSet<>(), Elapsed.end(ElapsedStartAt.now()));

        ElapsedStartAt start = ElapsedStartAt.now();

        String cleanedText = text.replaceAll("[^가-힣a-zA-Z\\s]", "");
        int currentPos = 0;
        Set<FilterWord> filterWords = new HashSet<>();

        for (Emit emit : trie.parseText(cleanedText)) {
            int startPos = text.indexOf(emit.getKeyword().charAt(0), currentPos);
            if (startPos == -1) continue;
            int endPos = startPos;
            for (char c : emit.getKeyword().toCharArray()) {
                endPos = text.indexOf(c, endPos) + 1;
            }
            filterWords.add(FilterWord.create(
                    text.substring(startPos, endPos),
                    startPos,
                    endPos
            ));
            currentPos = endPos;
        }
        Elapsed elapsed = Elapsed.end(start);
        log.info("전체 비속어 필터링 : {} (지연 시간 : {}ms)", text, elapsed);
        return FilterResponse.create(text, filterWords, elapsed);
    }

    @Override
    public FilterWord firstMatched(String text) {
        log.info("[NormalProfanityFilter] 단일 비속어 필터링 시작 : {}", LocalDateTime.now());
        if (text == null || text.isBlank()) {
            return FilterWord.empty();
        }

        ElapsedStartAt start = ElapsedStartAt.now();

        String cleanedText = text.replaceAll("[^가-힣a-zA-Z\\s]", "");
        Emit emit = trie.firstMatch(cleanedText);

        if (emit == null) {
            return FilterWord.empty();
        }

        int startPos = text.indexOf(emit.getKeyword().charAt(0));
        int endPos = startPos;
        for (char c : emit.getKeyword().toCharArray()) {
            endPos = text.indexOf(c, endPos) + 1;
        }

        Elapsed elapsed = Elapsed.end(start);
        log.info("단일 비속어 필터링 : {} (지연 시간 : {}ms)", text, elapsed);
        return FilterWord.create(text.substring(startPos, endPos), startPos, endPos);
    }
}
