package app.application.filter;

import app.core.data.response.FilterApiResponse;
import app.dto.request.FilterRequest;

import java.util.UUID;

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

    /**
     * 비동기 필터링 요청을 처리합니다.
     * 요청은 즉시 접수되고 trackingId를 반환하며, 처리 완료 후 콜백 URL로 결과가 전송됩니다.
     *
     * @param request     필터링 요청 객체
     * @param callbackUrl 결과를 전송할 콜백 URL
     * @return 요청 접수 상태와 trackingId를 포함한 응답
     */
    FilterApiResponse requestAsyncFilter(FilterRequest request, String callbackUrl);

    /**
     * 요청 처리 상태를 조회합니다.
     *
     * @param trackingId 요청 추적 ID
     * @return 현재 처리 상태를 포함한 응답
     */
    FilterApiResponse getFilterStatus(UUID trackingId);
}
