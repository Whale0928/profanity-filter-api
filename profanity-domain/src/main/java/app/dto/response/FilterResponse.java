package app.dto.response;

import app.core.data.elapsed.Elapsed;

import java.util.Set;

public record FilterResponse(
        String originalText,
        Set<FilterWord> filterWords,
        Elapsed elapsed
) {
    public static FilterResponse create(
            String originalText,
            Set<FilterWord> filterWords,
            Elapsed elapsed
    ) {
        return new FilterResponse(originalText, filterWords, elapsed);
    }
}
