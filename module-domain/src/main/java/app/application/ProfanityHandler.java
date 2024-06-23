package app.application;

import app.core.data.constant.Mode;
import app.core.data.response.ApiResponse;

public interface ProfanityHandler {

    /**
     * 모드에 따라 적절한 필터링을 수행합니다.
     *
     * @param word the word
     * @param mode the mode
     * @return the api response
     */
    ApiResponse requestFacadeFilter(String text, Mode mode);

    /**
     * 빠른 필터링을 수행합니다.
     * 비속어 발견 시 즉시 필터링을 종료합니다.
     *
     * @param word 검사 할 단어
     * @return the api response
     */
    ApiResponse quickFilter(String text);

    /**
     * 일반적인 필터링을 수행합니다.
     * 모든 비속어를 검사 하는 필터링을 수행합니다.
     * <p>
     * * @param word 검사 할 단어
     *
     * @return the api response
     */
    ApiResponse normalFilter(String text);

    /**
     * 일반적인 필터링을 수행합니다.
     * 모든 비속어를 검사 하는 필터링을 수행합니다.
     * 필터링 후 마스킹 처리를 수행합니다.
     *
     * @param word the word
     * @return the api response
     */
    ApiResponse sanitizeProfanity(String text);

    /**
     *
     *
     * @param text the text
     */
    ApiResponse advancedFilter(String text);
}
