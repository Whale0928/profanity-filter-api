package app.fixture;

import app.core.data.constant.Mode;
import app.dto.request.ApiRequest;

public class ApiTestFixture {

    public ApiRequest createQuickRequest(String text) {
        return new ApiRequest(text, Mode.QUICK, null);
    }

    public ApiRequest createNormalRequest(String text) {
        return new ApiRequest(text, Mode.NORMAL, null);
    }

    public ApiRequest createSanitizeRequest(String text) {
        return new ApiRequest(text, Mode.FILTER, null);
    }
}
