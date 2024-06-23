package app.application.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

public class FakeApplicationEventPublisher implements ApplicationEventPublisher {

    private static final Logger log = LogManager.getLogger(FakeApplicationEventPublisher.class);

    @Override
    public void publishEvent(ApplicationEvent event) {
        log.info("[ApplicationEvent]모의 이벤트 발행 : {}", event);
        ApplicationEventPublisher.super.publishEvent(event);
    }

    @Override
    public void publishEvent(Object event) {
        log.info("[Object]모의 이벤트 발행 : {}", event);
    }
}
