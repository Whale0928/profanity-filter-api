package app.application.filter;

import app.application.event.AsyncFilterEvent;
import app.application.event.FilterEvent;
import app.core.data.constant.Mode;
import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.core.data.response.Detected;
import app.core.data.response.FilterApiResponse;
import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.dto.request.FilterRequest;
import app.dto.response.FilterResponse;
import app.dto.response.FilterWord;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static app.core.data.response.constant.StatusCode.OK;

@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultProfanityHandler implements ProfanityHandler {

    private final NormalProfanityFilter normalProfanityFilter;
    private final ApplicationEventPublisher publisher;

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
        FilterWord filterWord = normalProfanityFilter.firstMatched(word);
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
        return sanitizeProfanity(text);
    }

    private Set<Detected> detects(Set<FilterWord> filterWords) {
        return filterWords.stream()
                .map(w -> Detected.of(w.length(), w.word()))
                .collect(Collectors.toSet());
    }

    private String masked(String word, Set<FilterWord> filterWords) {
        StringBuilder result = new StringBuilder(word);
        filterWords.forEach(f -> {
            int start = f.startIndex();
            int end = f.endIndex();
            result.replace(start, end, "*".repeat(end - start));
        });
        return result.toString();
    }

    private UUID generateTrackingId() {
        return UuidCreator.getTimeOrderedEpoch();
    }

    @Override
    @Transactional(readOnly = true)
    public FilterApiResponse requestAsyncFilter(FilterRequest request, String callbackUrl) {

        //1. 콜백 URL 유효성 검사 및 UUID 생성
        final UUID trackingId = generateTrackingId();
        final URI callbackUri;


        try {
            callbackUri = new URI(callbackUrl);
        } catch (URISyntaxException e) {
            log.error("잘못된 콜백 URL: {}", callbackUrl);
            throw new BusinessException(StatusCode.INVALID_CALLBACK_URL);
        }

        // 2. 수락 상태의 초기 응답 생성
        FilterApiResponse acceptedResponse = FilterApiResponse.builder()
                .trackingId(trackingId)
                .status(Status.of(StatusCode.ACCEPTED))
                .detected(Collections.emptySet())
                .filtered("")
                .elapsed(Elapsed.zero())
                .build();

        // 4. 비동기적으로 실제 필터링 수행
        CompletableFuture
                .supplyAsync(() -> {
                    log.info("백그라운드 필터링 처리 시작: trackingId={}", trackingId);
                    return requestFacadeFilter(request);
                })
                .thenAccept(response -> {
                    log.info("필터링 완료, 콜백 이벤트 발행: trackingId={}", trackingId);
                    publisher.publishEvent(AsyncFilterEvent.create(request, response, callbackUri));
                })
                .exceptionally(throwable -> {
                    log.error("비동기 필터링 처리 실패: trackingId={}, error={}", trackingId, throwable.getMessage(), throwable);
                    return null;
                });

        // 4. 즉시 수락 응답 반환
        return acceptedResponse;
    }
}
