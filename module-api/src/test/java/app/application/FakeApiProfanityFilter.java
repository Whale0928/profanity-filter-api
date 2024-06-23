package app.application;

import app.core.data.constant.Mode;
import app.core.data.response.ApiResponse;
import app.request.ApiRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FakeApiProfanityFilter implements ProfanityFilter {
    private static final Logger log = LogManager.getLogger(ProfanityService.class);
    private final ProfanityHandler filterService;

    public FakeApiProfanityFilter(ProfanityHandler filterService) {
        this.filterService = filterService;
    }

    @Override
    public ApiResponse basicFilter(String text, Mode mode) {
        log.info("[API] fake call : basicFilter");
        return filterService.requestFacadeFilter(text, mode);
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
