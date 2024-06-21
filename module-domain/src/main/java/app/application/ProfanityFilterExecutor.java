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

import java.util.List;
import java.util.UUID;

import static app.core.data.response.constant.StatusCode.OK;

@Service
public class ProfanityFilterExecutor implements ProfanityFilterService {

    private final QuickProfanityFilter quickProfanityFilter;
    private final NormalProfanityFilter normalProfanityFilter;
    private final AdvancedProfanityFilter advancedProfanityFilter;

    public ProfanityFilterExecutor(
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
        ElapsedStartAt start = ElapsedStartAt.now();
        Boolean profanity = quickProfanityFilter.containsProfanity(word);
        Elapsed end = Elapsed.end(start);

        if (profanity) {
            return ApiResponse.builder()
                    .trackingId(UUID.randomUUID())
                    .status(Status.of(OK))
                    .detected(List.of(Detected.of(word.length(), word)))
                    .filtered("")
                    .elapsed(end)
                    .build();
        }
        return normalFilter(word);
    }

    @Override
    public ApiResponse normalFilter(String word) {
        final FilterResponse filterResponse = normalProfanityFilter.allMatched(word);
        final List<Detected> detects = detects(filterResponse.filterWords());

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
        final List<Detected> detects = detects(filterResponse.filterWords());
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
    public void advancedFilter(String text) {
        advancedProfanityFilter.call();
    }

    private List<Detected> detects(List<FilterWord> filterWords) {
        return filterWords.stream()
                .map(w -> Detected.of(w.length(), w.word()))
                .toList();
    }

    private String masked(String word, List<FilterWord> filterWords) {
        return filterWords.stream()
                .reduce(word, (w, f) -> w.replace(f.word(), "*".repeat(f.length())), String::concat);
    }
}
