package app.application;

import app.application.filter.ProfanityHandler;
import app.core.data.response.ApiResponse;
import app.dto.request.FilterRequest;
import app.request.ApiRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ProfanityService implements ProfanityFilterService {

    private static final Logger log = LogManager.getLogger(ProfanityService.class);
    private final ProfanityHandler filterService;

    public ProfanityService(ProfanityHandler profanityHandler) {
        this.filterService = profanityHandler;
    }

    @Override
    public ApiResponse basicFilter(FilterRequest filterRequest) {
        log.info("[API] : request basicFilter: {}", filterRequest);
        return filterService.requestFacadeFilter(filterRequest);
    }

    @Override
    public ApiResponse advancedFilter(String text) {
        log.info("[API] : request advancedFilter: text={}", text);
        return filterService.advancedFilter(text);
    }

    @Override
    public Object healthCheck(ApiRequest request) {
        log.debug("[API] : request healthCheck: request={}", request);
        log.debug("async: {}", request.isAsync());
        return request;
    }
}
