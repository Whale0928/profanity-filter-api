package app.test.support.fake;

import app.application.filter.ProfanityHandler;
import app.core.data.constant.Mode;
import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.core.data.response.Detected;
import app.core.data.response.FilterApiResponse;
import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import app.dto.request.FilterRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FakeProfanityHandler implements ProfanityHandler {
  private static final List<String> PROFANITY_WORDS = List.of("비속어", "욕설");

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
    ElapsedStartAt startAt = ElapsedStartAt.now();

    Set<Detected> detectedSet =
        PROFANITY_WORDS.stream()
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
    ElapsedStartAt startAt = ElapsedStartAt.now();
    Set<Detected> detected =
        PROFANITY_WORDS.stream()
            .filter(text::contains)
            .map(m -> Detected.of(m.length(), m))
            .collect(Collectors.toSet());
    Elapsed ended = Elapsed.end(startAt);

    return FilterApiResponse.builder()
        .trackingId(UUID.randomUUID())
        .status(Status.of(StatusCode.OK))
        .detected(detected)
        .filtered("")
        .elapsed(ended)
        .build();
  }

  @Override
  public FilterApiResponse sanitizeProfanity(String text, UUID trackingId) {
    ElapsedStartAt startAt = ElapsedStartAt.now();
    Set<Detected> detected =
        PROFANITY_WORDS.stream()
            .filter(text::contains)
            .map(m -> Detected.of(m.length(), m))
            .collect(Collectors.toSet());
    Elapsed ended = Elapsed.end(startAt);

    for (String profanityWord : PROFANITY_WORDS) {
      text = text.replaceAll(profanityWord, "*".repeat(profanityWord.length()));
    }

    return FilterApiResponse.builder()
        .trackingId(UUID.randomUUID())
        .status(Status.of(StatusCode.OK))
        .detected(detected)
        .filtered(text)
        .elapsed(ended)
        .build();
  }

  @Override
  public FilterApiResponse advancedFilter(String text, UUID trackingId) {
    return sanitizeProfanity(text, trackingId);
  }

  @Override
  public FilterApiResponse requestAsyncFilter(FilterRequest request, String callbackUrl) {
    throw new UnsupportedOperationException("Async filter is not supported by fake handler");
  }
}
