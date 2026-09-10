package app.application.admin;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;

/** 발행된 이벤트를 기록해 두는 테스트 더블입니다. */
public class RecordingEventPublisher implements ApplicationEventPublisher {

  private final List<Object> events = new ArrayList<>();

  @Override
  public void publishEvent(Object event) {
    events.add(event);
  }

  public List<Object> events() {
    return List.copyOf(events);
  }

  public <T> List<T> eventsOf(Class<T> type) {
    return events.stream().filter(type::isInstance).map(type::cast).toList();
  }
}
