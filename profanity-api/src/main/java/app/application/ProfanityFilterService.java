package app.application;

import app.core.data.response.ApiResponse;
import app.dto.request.FilterRequest;
import app.request.ApiRequest;

/**
 * API모듈 입장에서 Filter 도메인을 연결하는 Connector Adapter
 */
public interface ProfanityFilterService {
    ApiResponse basicFilter(FilterRequest filterRequest);

    ApiResponse advancedFilter(String word);

    Object healthCheck(ApiRequest request);
}
