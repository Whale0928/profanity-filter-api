package app.application;

import app.application.filter.ProfanityHandler;
import app.core.data.response.ApiResponse;
import app.dto.request.ApiRequest;
import app.dto.request.FilterRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FakeApiProfanityFilter implements ProfanityFilterService {
    private static final Logger log = LogManager.getLogger(ProfanityService.class);
    private final ProfanityHandler filterService;

    public FakeApiProfanityFilter(ProfanityHandler filterService) {
        this.filterService = filterService;
    }

    @Override
    public ApiResponse basicFilter(FilterRequest filterRequest) {
        log.info("[API] fake call : basicFilter");
        return filterService.requestFacadeFilter(filterRequest);
    }

    @Override
    public ApiResponse advancedFilter(String word) {
        log.info("[API] fake call : advancedFilter");
        return null;
    }

    @Override
    public ApiRequest healthCheck(ApiRequest request) {
        System.out.println("[API] fake call : healthCheck");
        return request;
    }
}
