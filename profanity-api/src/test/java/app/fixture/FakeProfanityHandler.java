package app.fixture;

import app.application.filter.ProfanityHandler;
import app.core.data.constant.Mode;
import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.core.data.response.Detected;
import app.core.data.response.FilterApiResponse;
import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import app.dto.request.FilterRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FakeProfanityHandler implements ProfanityHandler {
    private static final List<String> profaityWordList = List.of("비속어", "욕설");
    private static final Logger log = LogManager.getLogger(FakeProfanityHandler.class);

    @Override
    public FilterApiResponse requestFacadeFilter(FilterRequest filterRequest, UUID trackingId) {
        Mode mode = filterRequest.mode();
        String text = filterRequest.text();

        return switch (mode) {
            case QUICK -> quickFilter(text, trackingId);
            case NORMAL -> normalFilter(text, trackingId);
            case FILTER -> sanitizeProfanity(text, trackingId);
        };
    }

    @Override
    public FilterApiResponse quickFilter(String text, UUID trackingId) {
        log.info("[domain] fake call : quickFilter");

        ElapsedStartAt startAt = ElapsedStartAt.now();

        Set<Detected> detectedSet = profaityWordList.stream()
                .filter(text::contains)
                .findFirst()
                .map(m -> Collections.singleton(Detected.of(m.length(), m)))
                .orElse(Collections.emptySet());

        Elapsed ended = Elapsed.end(startAt);

        return FilterApiResponse.builder()
                .trackingId(UUID.randomUUID())
                .status(Status.of(StatusCode.OK))
                .detected(detectedSet)
                .filtered("")
                .elapsed(ended)
                .build();
    }

    @Override
    public FilterApiResponse normalFilter(String text, UUID trackingId) {
        log.info("[domain] fake call : normalFilter");
        ElapsedStartAt startAt = ElapsedStartAt.now();
        Set<Detected> collect = profaityWordList.stream()
                .filter(text::contains)
                .map(m -> Detected.of(m.length(), m))
                .collect(Collectors.toSet());
        Elapsed ended = Elapsed.end(startAt);

        return FilterApiResponse.builder()
                .trackingId(UUID.randomUUID())
                .status(Status.of(StatusCode.OK))
                .detected(collect)
                .filtered("")
                .elapsed(ended)
                .build();
    }

    @Override
    public FilterApiResponse sanitizeProfanity(String text, UUID trackingId) {

        log.info("[domain] fake call : sanitizeProfanity");

        ElapsedStartAt startAt = ElapsedStartAt.now();
        Set<Detected> collect = profaityWordList.stream()
                .filter(text::contains)
                .map(m -> Detected.of(m.length(), m))
                .collect(Collectors.toSet());
        Elapsed ended = Elapsed.end(startAt);

        for (String profaityWord : profaityWordList) {
            text = text.replaceAll(profaityWord, "*".repeat(profaityWord.length()));
        }

        return FilterApiResponse.builder()
                .trackingId(UUID.randomUUID())
                .status(Status.of(StatusCode.OK))
                .detected(collect)
                .filtered(text)
                .elapsed(ended)
                .build();
    }

    @Override
    public FilterApiResponse advancedFilter(String text, UUID trackingId) {
        log.info("[domain]  fake call : advancedFilter");
        return sanitizeProfanity(text, trackingId);
    }

    @Override
    public FilterApiResponse requestAsyncFilter(FilterRequest request, String callbackUrl) {
        //todo : 비동기 처리
        return null;
    }

}
