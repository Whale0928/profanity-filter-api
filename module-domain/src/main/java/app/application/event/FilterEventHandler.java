package app.application.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FilterEventHandler {


    private static final Logger log = LogManager.getLogger(FilterEventHandler.class);

    @Transactional
    @TransactionalEventListener
    public void handle(FilterEvent event) {
        log.info("[DOMAIN] 이벤트 발행 성공 : event={}", event);
    }
}
