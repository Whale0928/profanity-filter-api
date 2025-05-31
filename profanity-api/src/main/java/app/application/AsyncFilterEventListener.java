package app.application;

import app.application.event.AsyncFilterEvent;
import app.core.data.response.FilterApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncFilterEventListener {
    private final RestClient restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    /**
     * 비동기 필터링 이벤트 처리
     * 콜백 URL로 필터링 결과를 전송합니다.
     */
    @Async
    @EventListener
    public void handleAsyncFilterEvent(AsyncFilterEvent event) {
        URI callbackUrl = event.getCallbackUrl();
        FilterApiResponse response = event.getResponse();
        log.info("비동기 필터링 완료, 콜백 호출 시작: {}", callbackUrl);

        try {
            //3초대기

            // RestClient로 POST 요청 실행
            ResponseEntity<Void> responseEntity = restClient.post()
                    .uri(callbackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response)
                    .retrieve()
                    .toEntity(Void.class);

            log.info("콜백 호출 성공: {}, trackingId: {}, status: {}",
                    callbackUrl, response.trackingId(), responseEntity.getStatusCode());

        } catch (Exception e) {
            log.error("콜백 호출 실패: {}, trackingId: {}, 오류: {}",
                    callbackUrl, response.trackingId(), e.getMessage(), e);
            // 여기에 필요시 재시도 로직 추가
        }
    }
}
