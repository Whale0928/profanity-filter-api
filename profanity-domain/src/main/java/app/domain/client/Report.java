package app.domain.client;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import app.core.data.Const;
import app.domain.apikey.ApiKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 기존 일일 API Key 사용 리포트입니다.
 *
 * @deprecated 수집 중단을 검토 중입니다. 신규 기능은 이 모델에 의존하지 않아야 합니다.
 */
@Deprecated(forRemoval = true)
@Getter
@Builder
@ToString(
    of = {
      "id",
      "clientId",
      "reportYear",
      "reportMonth",
      "reportDay",
      "requestCount",
      "profanityDetectionCount"
    })
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "client_reports")
@Table(name = "client_reports")
public class Report {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Comment("리포트 ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  @Comment("클라이언트")
  private ApiKey apiKey;

  @Column(name = "client_id", insertable = false, updatable = false)
  @Comment("클라이언트 ID")
  private UUID clientId;

  @Builder.Default
  @Column(name = "report_year", nullable = false)
  @Comment("리포트 년도")
  private Integer reportYear = Const.getCurrentYear();

  @Builder.Default
  @Column(name = "report_month", nullable = false)
  @Comment("리포트 월")
  private Integer reportMonth = Const.getCurrentMonth();

  @Builder.Default
  @Column(name = "report_day", nullable = false)
  @Comment("리포트 일")
  private Integer reportDay = Const.getCurrentDay();

  @Builder.Default
  @Column(name = "request_count", nullable = false)
  @Comment("일일 요청 횟수")
  private Long requestCount = 0L;

  @Builder.Default
  @Column(name = "profanity_detection_count", nullable = false)
  @Comment("욕설 검출 횟수")
  private Long profanityDetectionCount = 0L;

  @Builder.Default
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  @Comment("생성 시각")
  private LocalDateTime createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

  /**
   * 특정 클라이언트의 오늘 리포트 생성
   *
   * @param apiKey API Key
   * @return 새 리포트
   */
  public static Report createTodayReport(ApiKey apiKey) {
    return Report.builder()
        .apiKey(apiKey)
        .clientId(apiKey.getId())
        .reportYear(Const.getCurrentYear())
        .reportMonth(Const.getCurrentMonth())
        .reportDay(Const.getCurrentDay())
        .build();
  }

  /**
   * 요청 횟수 업데이트
   *
   * @param requestCount 요청 횟수
   * @param profanityDetectionCount 욕설 검출 횟수
   */
  public void updateCounts(Long requestCount, Long profanityDetectionCount) {
    this.requestCount = requestCount;
    this.profanityDetectionCount = profanityDetectionCount;
  }
}
