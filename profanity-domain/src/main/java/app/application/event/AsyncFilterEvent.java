package app.application.event;


import app.core.data.response.FilterApiResponse;
import app.dto.request.FilterRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.net.URI;

/**
 * 비동기 필터링 완료 후 콜백 처리를 위한 이벤트
 */
@Getter
public class AsyncFilterEvent extends ApplicationEvent {

    private final FilterRequest request;
    private final FilterApiResponse response;
    private final URI callbackUrl;

    /**
     * 비동기 필터링 이벤트 생성
     *
     * @param request     원본 필터링 요청
     * @param response    필터링 결과 응답
     * @param callbackUrl 결과를 전송할 콜백 URL
     */
    public AsyncFilterEvent(FilterRequest request, FilterApiResponse response, URI callbackUrl) {
        super(response); // 이벤트 소스로 응답 객체 사용
        this.request = request;
        this.response = response;
        this.callbackUrl = callbackUrl;
    }

    /**
     * 정적 팩토리 메서드
     */
    public static AsyncFilterEvent create(FilterRequest request, FilterApiResponse response, URI callbackUrl) {
        return new AsyncFilterEvent(request, response, callbackUrl);
    }
}
