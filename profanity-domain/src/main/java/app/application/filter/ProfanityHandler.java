package app.application.filter;

import app.core.data.response.FilterApiResponse;
import app.dto.request.FilterRequest;

import java.util.UUID;

public interface ProfanityHandler {

    FilterApiResponse requestFacadeFilter(FilterRequest request, UUID trackingId);

    FilterApiResponse quickFilter(String text, UUID trackingId);

    FilterApiResponse normalFilter(String text, UUID trackingId);

    FilterApiResponse sanitizeProfanity(String text, UUID trackingId);

    FilterApiResponse advancedFilter(String text, UUID trackingId);

    FilterApiResponse requestAsyncFilter(FilterRequest request, String callbackUrl);
}
