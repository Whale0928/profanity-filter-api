package app.application.filter;

import app.core.data.response.FilterApiResponse;
import app.dto.request.FilterRequest;

import java.util.UUID;

public class FackProfanityHandler implements ProfanityHandler {
    @Override
    public FilterApiResponse requestFacadeFilter(FilterRequest request, UUID trackingId) {
        return null;
    }

    @Override
    public FilterApiResponse quickFilter(String text, UUID trackingId) {
        return null;
    }

    @Override
    public FilterApiResponse normalFilter(String text, UUID trackingId) {
        return null;
    }

    @Override
    public FilterApiResponse sanitizeProfanity(String text, UUID trackingId) {
        return null;
    }

    @Override
    public FilterApiResponse advancedFilter(String text, UUID trackingId) {
        return null;
    }

    @Override
    public FilterApiResponse requestAsyncFilter(FilterRequest request, String callbackUrl) {
        return null;
    }
}
