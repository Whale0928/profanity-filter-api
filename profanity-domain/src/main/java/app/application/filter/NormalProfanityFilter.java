package app.application.filter;

import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.domain.profanity.ProfanityRepository;
import app.domain.profanity.ProfanityWord;
import app.domain.profanity.constant.isUsedType;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NormalProfanityFilter implements ProfanityFilter, AhocorasickFilter {
  private static volatile Trie trie;
  private final ProfanityRepository profanityRepository;
  private Set<String> collect = new HashSet<>();

  @PostConstruct
  public void postConstruct() {
    synchronizeProfanityTrie();
  }

  @Override
  public boolean synchronizeProfanityTrie() {
    ElapsedStartAt start = ElapsedStartAt.now();
    // is_used = 'N' 단어는 관리자가 비활성화한 단어이므로 매칭 대상에서 제외한다.
    Set<String> newWords =
        profanityRepository.findAllByIsUsed(isUsedType.Y).stream()
            .map(ProfanityWord::getWord)
            .collect(Collectors.toSet());
    Trie newTrie = Trie.builder().ignoreOverlaps().ignoreCase().addKeywords(newWords).build();

    boolean changed = !newWords.equals(this.collect);

    // 원자적 재할당
    this.collect = newWords;
    trie = newTrie;

    log.info(
        "[AhocorasickFilter] 비속어 자료 로딩 완료 {}개 변경여부={} (지연 시간 : {}ms)",
        collect.size(),
        changed,
        Elapsed.end(start));
    return changed;
  }

  @Override
  public List<?> getProfanityTrieList() {
    log.debug("[AhocorasickFilter] 비속어 자료 목록 조회 : {}", LocalDateTime.now());
    return collect.stream().toList();
  }

  @Override
  public Boolean containsProfanity(String text) {
    return !trie.parseText(text).isEmpty();
  }

  @Override
  public FilterResponse allMatched(String text) {
    return allMatched(text, Set.of());
  }

  @Override
  public FilterResponse allMatched(String text, Set<String> allowedWords) {
    log.debug("[NormalProfanityFilter] 전체 비속어 필터링 시작");

    if (text == null || text.isBlank())
      return FilterResponse.create(text, new HashSet<>(), Elapsed.end(ElapsedStartAt.now()));

    ElapsedStartAt start = ElapsedStartAt.now();

    String cleanedText = ProfanityText.clean(text);
    int currentPos = 0;
    Set<FilterWord> filterWords = new HashSet<>();

    for (Emit emit : trie.parseText(cleanedText)) {
      int startPos = text.indexOf(emit.getKeyword().charAt(0), currentPos);
      if (startPos == -1) continue;
      int endPos = startPos;
      for (char c : emit.getKeyword().toCharArray()) {
        endPos = text.indexOf(c, endPos) + 1;
      }
      // 허용 단어도 원문 위치는 그대로 지나가야 뒤따르는 검출의 위치가 어긋나지 않는다.
      currentPos = endPos;
      if (isAllowed(emit, allowedWords)) continue;
      filterWords.add(FilterWord.create(text.substring(startPos, endPos), startPos, endPos));
    }
    Elapsed elapsed = Elapsed.end(start);
    log.debug("[NormalProfanityFilter] 전체 비속어 필터링 완료 (지연 시간 : {})", elapsed);
    return FilterResponse.create(text, filterWords, elapsed);
  }

  @Override
  public FilterWord firstMatched(String text) {
    log.debug("[NormalProfanityFilter] 단일 비속어 필터링 시작");
    if (text == null || text.isBlank()) {
      return FilterWord.empty();
    }

    ElapsedStartAt start = ElapsedStartAt.now();

    String cleanedText = ProfanityText.clean(text);
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
    log.debug("[NormalProfanityFilter] 단일 비속어 필터링 완료 (지연 시간 : {})", elapsed);
    return FilterWord.create(text.substring(startPos, endPos), startPos, endPos);
  }

  /** 첫 검출이 허용 단어이면 그 뒤의 검출을 찾아야 하므로 전체를 훑습니다. 결과를 뒤에서 걸러 내면 허용 단어 뒤에 있는 실제 비속어를 놓칩니다. */
  @Override
  public FilterWord firstMatched(String text, Set<String> allowedWords) {
    if (allowedWords == null || allowedWords.isEmpty()) {
      return firstMatched(text);
    }
    if (text == null || text.isBlank()) {
      return FilterWord.empty();
    }

    String cleanedText = ProfanityText.clean(text);
    int currentPos = 0;
    for (Emit emit : trie.parseText(cleanedText)) {
      int startPos = text.indexOf(emit.getKeyword().charAt(0), currentPos);
      if (startPos == -1) continue;
      int endPos = startPos;
      for (char c : emit.getKeyword().toCharArray()) {
        endPos = text.indexOf(c, endPos) + 1;
      }
      currentPos = endPos;
      if (isAllowed(emit, allowedWords)) continue;
      return FilterWord.create(text.substring(startPos, endPos), startPos, endPos);
    }
    return FilterWord.empty();
  }

  private static boolean isAllowed(Emit emit, Set<String> allowedWords) {
    return allowedWords != null
        && !allowedWords.isEmpty()
        && allowedWords.contains(ProfanityText.comparisonKey(emit.getKeyword()));
  }
}
