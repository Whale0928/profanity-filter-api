package app.application;

import app.core.data.response.ApiResponse;
import app.dto.request.FilterRequest;
import app.request.ApiRequest;

public interface ProfanityFilterService {
    ApiResponse basicFilter(FilterRequest filterRequest);

    ApiResponse advancedFilter(String word);

    Object healthCheck(ApiRequest request);
}
