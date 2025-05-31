package app.application.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FilterEventHandler {
    private final TrackingRecorder trackingRecorder;

    public FilterEventHandler(TrackingRecorder trackingRecorder) {
        this.trackingRecorder = trackingRecorder;
    }

    //@Transactional(propagation = REQUIRES_NEW)
    //@TransactionalEventListener
    @EventListener
    public void handle(FilterEvent event) {
        trackingRecorder.recordTracking(event);
        log.info("[DOMAIN] 이벤트 발행 성공 : event={}", event);
    }
}
