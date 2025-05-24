package app.application;

import app.application.event.AsyncFilterEvent;
import app.core.data.response.FilterApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncFilterEventListener {
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 비동기 필터링 이벤트 처리
     * 콜백 URL로 필터링 결과를 전송합니다.
     */
    @Async
    @EventListener
    public void handleAsyncFilterEvent(AsyncFilterEvent event) {
        String callbackUrl = event.getCallbackUrl();
        FilterApiResponse response = event.getResponse();
        log.info("비동기 필터링 완료, 콜백 호출 시작: {}", callbackUrl);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<FilterApiResponse> requestEntity = new HttpEntity<>(response, headers);
            restTemplate.postForEntity(callbackUrl, requestEntity, Void.class);

            log.info("콜백 호출 성공: {}, trackingId: {}", callbackUrl, response.trackingId());
        } catch (Exception e) {
            log.error("콜백 호출 실패: {}, trackingId: {}, 오류: {}",
                    callbackUrl, response.trackingId(), e.getMessage(), e);
            // 여기에 필요시 재시도 로직 추가
        }
    }

}
