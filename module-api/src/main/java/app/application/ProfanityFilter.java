package app.application;

import app.core.data.constant.Mode;
import app.core.data.response.ApiResponse;
import app.request.ApiRequest;
import jakarta.validation.constraints.NotNull;

public interface ProfanityFilter {
    ApiResponse basicFilter(
            @NotNull(message = "검사할 대상은 필수입니다.") String text,
            @NotNull(message = "검사 방식은 필수입니다.") Mode mode
    );

    ApiResponse advancedFilter(String word);

    Object healthCheck(ApiRequest request);
}
