package app.dto.response;

import app.core.data.elapsed.Elapsed;

import java.util.List;

public record FilterResponse(
        // 기존 문자열
        String originalText,
        List<FilterWord> filterWords,
        Elapsed elapsed
) {
    public static FilterResponse create(
            String originalText,
            List<FilterWord> filterWords,
            Elapsed elapsed
    ) {
        return new FilterResponse(originalText, filterWords, elapsed);
    }
}
