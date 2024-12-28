package app.application.filter;

import app.application.event.FilterEvent;
import app.core.data.constant.Mode;
import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.core.data.response.Detected;
import app.core.data.response.FilterApiResponse;
import app.core.data.response.Status;
import app.dto.request.FilterRequest;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import com.github.f4b6a3.uuid.UuidCreator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static app.core.data.response.constant.StatusCode.OK;

@Service
public class DefaultProfanityHandler implements ProfanityHandler {

    private static final Logger log = LogManager.getLogger(DefaultProfanityHandler.class);
    private final QuickProfanityFilter quickProfanityFilter;
    private final NormalProfanityFilter normalProfanityFilter;
    private final AdvancedProfanityFilter advancedProfanityFilter;
    private final ApplicationEventPublisher publisher;

    public DefaultProfanityHandler(
            QuickProfanityFilter quickProfanityFilter,
            NormalProfanityFilter normalProfanityFilter,
            AdvancedProfanityFilter advancedProfanityFilter,
            ApplicationEventPublisher publisher
    ) {
        this.quickProfanityFilter = quickProfanityFilter;
        this.normalProfanityFilter = normalProfanityFilter;
        this.advancedProfanityFilter = advancedProfanityFilter;
        this.publisher = publisher;
    }

    @Override
    @Transactional(readOnly = true)
    public FilterApiResponse requestFacadeFilter(FilterRequest request) {
        Mode mode = request.mode();
        String text = request.text();

        log.info("[DOMAIN] requestFacadeFilter : request={}", request);

        FilterApiResponse response = switch (mode) {
            case QUICK -> quickFilter(text);
            case NORMAL -> normalFilter(text);
            case FILTER -> sanitizeProfanity(text);
        };

        publisher.publishEvent(FilterEvent.create(request, response));

        return response;
    }

    @Override
    public FilterApiResponse quickFilter(String word) {
        ElapsedStartAt start = ElapsedStartAt.now();
        FilterWord filterWord = quickProfanityFilter.firstMatched(word);
        Elapsed elapsed = Elapsed.end(start);

        Set<Detected> detected = Set.of(Detected.of(filterWord.length(), filterWord.word()));

        return FilterApiResponse.builder()
                .trackingId(generateTrackingId())
                .status(Status.of(OK))
                .detected(detected)
                .filtered("")
                .elapsed(elapsed)
                .build();
    }

    @Override
    public FilterApiResponse normalFilter(String word) {
        final FilterResponse filterResponse = normalProfanityFilter.allMatched(word);
        final Set<Detected> detects = detects(filterResponse.filterWords());

        return FilterApiResponse.builder()
                .trackingId(generateTrackingId())
                .status(Status.of(OK))
                .detected(detects)
                .filtered("")
                .elapsed(filterResponse.elapsed())
                .build();
    }

    @Override
    public FilterApiResponse sanitizeProfanity(String word) {
        final FilterResponse filterResponse = normalProfanityFilter.allMatched(word);
        final Set<Detected> detects = detects(filterResponse.filterWords());
        final String masked = masked(word, filterResponse.filterWords());

        return FilterApiResponse.builder()
                .trackingId(generateTrackingId())
                .status(Status.of(OK))
                .detected(detects)
                .filtered(masked)
                .elapsed(filterResponse.elapsed())
                .build();
    }

    @Override
    public FilterApiResponse advancedFilter(String text) {
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

    private UUID generateTrackingId() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
