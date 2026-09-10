package app.application.manage;

import static org.assertj.core.api.Assertions.assertThat;

import app.application.admin.RecordingEventPublisher;
import app.application.filter.AhocorasickFilter;
import app.application.filter.ProfanityDictionaryReloadedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SyncSchedulerTest {

  private static final Instant NOW = Instant.parse("2026-09-10T03:00:00Z");

  private final RecordingEventPublisher eventPublisher = new RecordingEventPublisher();

  @Test
  @DisplayName("단어 수가 그대로여도 매 주기마다 사전을 다시 읽는다")
  void synchronizeProfanityData_alwaysReloadsDictionary() {
    CountingFilter filter = new CountingFilter(false, false, false);
    SyncScheduler scheduler = scheduler(filter);

    scheduler.synchronizeProfanityData();
    scheduler.synchronizeProfanityData();
    scheduler.synchronizeProfanityData();

    assertThat(filter.calls()).isEqualTo(3);
  }

  @Test
  @DisplayName("적재 단어가 실제로 바뀐 주기에만 캐시 무효화 이벤트를 발행한다")
  void synchronizeProfanityData_publishesReloadOnlyWhenChanged() {
    CountingFilter filter = new CountingFilter(false, true, false);
    SyncScheduler scheduler = scheduler(filter);

    scheduler.synchronizeProfanityData();
    scheduler.synchronizeProfanityData();
    scheduler.synchronizeProfanityData();

    List<ProfanityDictionaryReloadedEvent> events =
        eventPublisher.eventsOf(ProfanityDictionaryReloadedEvent.class);
    assertThat(events).hasSize(1);
    assertThat(events.get(0).reloadedAt()).isEqualTo(NOW);
  }

  private SyncScheduler scheduler(AhocorasickFilter filter) {
    return new SyncScheduler(filter, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  /** 지정한 순서대로 변경 여부를 돌려주는 테스트 더블입니다. */
  private static final class CountingFilter implements AhocorasickFilter {
    private final Deque<Boolean> changes = new ArrayDeque<>();
    private int calls;

    private CountingFilter(Boolean... changes) {
      this.changes.addAll(List.of(changes));
    }

    @Override
    public boolean synchronizeProfanityTrie() {
      calls++;
      Boolean changed = changes.poll();
      return changed != null && changed;
    }

    @Override
    public List<?> getProfanityTrieList() {
      return List.of();
    }

    int calls() {
      return calls;
    }
  }
}
