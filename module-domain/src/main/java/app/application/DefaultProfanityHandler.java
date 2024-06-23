package app.application;

import app.core.data.constant.Mode;
import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.core.data.response.ApiResponse;
import app.core.data.response.Detected;
import app.core.data.response.Status;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static app.core.data.response.constant.StatusCode.OK;
import static java.util.Collections.emptySet;

@Service
public class DefaultProfanityHandler implements ProfanityHandler {

    private final QuickProfanityFilter quickProfanityFilter;
    private final NormalProfanityFilter normalProfanityFilter;
    private final AdvancedProfanityFilter advancedProfanityFilter;

    public DefaultProfanityHandler(
            QuickProfanityFilter quickProfanityFilter,
            NormalProfanityFilter normalProfanityFilter,
            AdvancedProfanityFilter advancedProfanityFilter
    ) {
        this.quickProfanityFilter = quickProfanityFilter;
        this.normalProfanityFilter = normalProfanityFilter;
        this.advancedProfanityFilter = advancedProfanityFilter;
    }

    @Override
    public ApiResponse requestFacadeFilter(String word, Mode mode) {
        return switch (mode) {
            case QUICK -> quickFilter(word);
            case NORMAL -> normalFilter(word);
            case FILTER -> sanitizeProfanity(word);
        };
    }

    @Override
    public ApiResponse quickFilter(String word) {
        ElapsedStartAt outBoundStart = ElapsedStartAt.now();
        Boolean profanity = quickProfanityFilter.containsProfanity(word);
        Elapsed outBoundlapsed = Elapsed.end(outBoundStart);

        if (profanity) {
            ElapsedStartAt start = ElapsedStartAt.now();
            FilterWord filterWord = normalProfanityFilter.firstMatched(word);
            Elapsed elapsed = Elapsed.end(start);

            Set<Detected> detected = Set.of(Detected.of(filterWord.length(), filterWord.word()));

            return ApiResponse.builder()
                    .trackingId(UUID.randomUUID())
                    .status(Status.of(OK))
                    .detected(detected)
                    .filtered("")
                    .elapsed(elapsed)
                    .build();
        }

        return ApiResponse.builder()
                .trackingId(UUID.randomUUID())
                .status(Status.of(OK))
                .detected(emptySet())
                .filtered("")
                .elapsed(outBoundlapsed)
                .build();
    }

    @Override
    public ApiResponse normalFilter(String word) {
        final FilterResponse filterResponse = normalProfanityFilter.allMatched(word);
        final Set<Detected> detects = detects(filterResponse.filterWords());

        return ApiResponse.builder()
                .trackingId(UUID.randomUUID())
                .status(Status.of(OK))
                .detected(detects)
                .filtered("")
                .elapsed(filterResponse.elapsed())
                .build();
    }

    @Override
    public ApiResponse sanitizeProfanity(String word) {
        final FilterResponse filterResponse = normalProfanityFilter.allMatched(word);
        final Set<Detected> detects = detects(filterResponse.filterWords());
        final String masked = masked(word, filterResponse.filterWords());

        return ApiResponse.builder()
                .trackingId(UUID.randomUUID())
                .status(Status.of(OK))
                .detected(detects)
                .filtered(masked)
                .elapsed(filterResponse.elapsed())
                .build();
    }

    @Override
    public ApiResponse advancedFilter(String text) {
        advancedProfanityFilter.call();
        return sanitizeProfanity(text);
    }

    private Set<Detected> detects(Set<FilterWord> filterWords) {
        return filterWords.stream()
                .map(w -> Detected.of(w.length(), w.word()))
                .collect(Collectors.toSet());
    }

    private String masked(String word, Set<FilterWord> filterWords) {
        return filterWords.stream()
                .reduce(word, (w, f) -> w.replace(f.word(), "*".repeat(f.length())), String::concat);
    }
}
