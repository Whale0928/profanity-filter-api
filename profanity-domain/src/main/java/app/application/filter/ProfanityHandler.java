package app.application.filter;

import app.core.data.response.FilterApiResponse;
import app.dto.request.FilterRequest;

public interface ProfanityHandler {

    /**
     * 모드에 따라 적절한 필터링을 수행합니다.
     *
     * @param request 요청 객체
     * @return the api response
     */
    FilterApiResponse requestFacadeFilter(FilterRequest request);

    /**
     * 빠른 필터링을 수행합니다.
     * 비속어 발견 시 즉시 필터링을 종료합니다.
     *
     * @param text 검사 할 단어
     * @return the api response
     */
    FilterApiResponse quickFilter(String text);

    /**
     * 일반적인 필터링을 수행합니다.
     * 모든 비속어를 검사 하는 필터링을 수행합니다.
     * <p>
     * * @param text 검사 할 단어
     *
     * @return the api response
     */
    FilterApiResponse normalFilter(String text);

    /**
     * 일반적인 필터링을 수행합니다.
     * 모든 비속어를 검사 하는 필터링을 수행합니다.
     * 필터링 후 마스킹 처리를 수행합니다.
     *
     * @param text the text
     * @return the api response
     */
    FilterApiResponse sanitizeProfanity(String text);

    /**
     * @param text the text
     */
    FilterApiResponse advancedFilter(String text);
}
