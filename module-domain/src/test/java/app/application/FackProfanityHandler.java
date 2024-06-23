package app.application;

import app.core.data.constant.Mode;
import app.core.data.response.ApiResponse;

public class FackProfanityHandler implements ProfanityHandler {
    @Override
    public ApiResponse requestFacadeFilter(String text, Mode mode) {
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
