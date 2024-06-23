package app.application;

import app.core.data.constant.Mode;
import app.core.data.response.ApiResponse;
import app.request.ApiRequest;

public interface ProfanityFilterService {
    ApiResponse basicFilter(String text, Mode mode);

    ApiResponse advancedFilter(String word);

    Object healthCheck(ApiRequest request);
}
