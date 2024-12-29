package app.application.filter;

import app.core.data.response.FilterApiResponse;
import app.dto.request.FilterRequest;

public class FackProfanityHandler implements ProfanityHandler {
    @Override
    public FilterApiResponse requestFacadeFilter(FilterRequest filterRequest) {
        return null;
    }

    @Override
    public FilterApiResponse quickFilter(String text) {
        return null;
    }

    @Override
    public FilterApiResponse normalFilter(String text) {
        return null;
    }

    @Override
    public FilterApiResponse sanitizeProfanity(String text) {
        return null;
    }

    @Override
    public FilterApiResponse advancedFilter(String text) {
        return null;
    }
}
