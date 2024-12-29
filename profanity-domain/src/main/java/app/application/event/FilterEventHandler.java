package app.application.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Component
public class FilterEventHandler {

    private static final Logger log = LogManager.getLogger(FilterEventHandler.class);
    private final TrackingRecorder trackingRecorder;

    public FilterEventHandler(TrackingRecorder trackingRecorder) {
        this.trackingRecorder = trackingRecorder;
    }

    @Transactional(propagation = REQUIRES_NEW)
    @TransactionalEventListener
    public void handle(FilterEvent event) {
        trackingRecorder.recordTracking(event);
        log.info("[DOMAIN] 이벤트 발행 성공 : event={}", event);
    }
}
