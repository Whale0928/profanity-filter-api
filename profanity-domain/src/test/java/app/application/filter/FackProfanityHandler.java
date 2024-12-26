package app.application.filter;

import app.core.data.response.ApiResponse;
import app.dto.request.FilterRequest;

public class FackProfanityHandler implements ProfanityHandler {
    @Override
    public ApiResponse requestFacadeFilter(FilterRequest filterRequest) {
        return null;
    }

    @Override
    public ApiResponse quickFilter(String text) {
        return null;
    }

    @Override
    public ApiResponse normalFilter(String text) {
        return null;
    }

    @Override
    public ApiResponse sanitizeProfanity(String text) {
        return null;
    }

    @Override
    public ApiResponse advancedFilter(String text) {
        return null;
    }
}
