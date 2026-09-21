package app.application.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리자 통계 개요 화면이 한 번에 사용하는 응답입니다.
 *
 * <p>기간에 따라 달라지는 값과 현재 시점의 누적 값을 구분해서 담습니다. summary의 요청 수는 기간 집계이고, API Key 수와 사전 단어 수는 기간과 무관한 누적
 * 값입니다.
 *
 * @param period 집계에 사용한 기간
 * @param summary 요약 지표
 * @param daily 일자별 요청 추이. 요청이 없던 날도 0으로 채워 기간 전체를 담습니다.
 * @param modes 필터링 모드별 요청 분포. 요청이 없던 모드도 0으로 포함합니다.
 * @param operations 운영 현황
 * @param topApiKeys 요청이 많은 API Key 목록
 */
public record AdminStatisticsView(
    PeriodView period,
    SummaryView summary,
    List<DailyView> daily,
    List<ModeView> modes,
    OperationsView operations,
    List<TopApiKeyView> topApiKeys) {

  /**
   * 집계 기간입니다. 경계는 서비스 시간대(Asia/Seoul)의 자정을 기준으로 합니다.
   *
   * @param days 조회한 일수
   * @param from 시작 일자(포함)
   * @param to 종료 일자(포함). 오늘입니다.
   */
  public record PeriodView(int days, LocalDate from, LocalDate to) {}

  /**
   * 요약 지표입니다.
   *
   * @param totalRequests 기간 내 기록된 요청 수
   * @param detectedRequests 기간 내 비속어가 검출된 요청 수
   * @param detectionRate 검출률(%). 소수점 첫째 자리까지입니다.
   * @param activeApiKeys 만료되지 않은 API Key 수
   * @param totalApiKeys 발급된 전체 API Key 수
   * @param usedWords 사용 중인 사전 단어 수
   * @param totalWords 전체 사전 단어 수
   */
  public record SummaryView(
      long totalRequests,
      long detectedRequests,
      double detectionRate,
      long activeApiKeys,
      long totalApiKeys,
      long usedWords,
      long totalWords) {}

  /**
   * 하루치 요청 추이입니다.
   *
   * @param date 서비스 시간대 기준 일자
   * @param totalRequests 그날 기록된 요청 수
   * @param detectedRequests 그날 비속어가 검출된 요청 수
   */
  public record DailyView(LocalDate date, long totalRequests, long detectedRequests) {}

  /**
   * 필터링 모드별 요청 분포입니다.
   *
   * @param mode 모드 이름
   * @param totalRequests 해당 모드의 요청 수
   * @param share 기간 내 전체 요청 대비 비중(%). 소수점 첫째 자리까지입니다.
   */
  public record ModeView(String mode, long totalRequests, double share) {}

  /**
   * 운영 현황입니다. 앞의 두 값은 현재 쌓여 있는 처리 대기 건수이고, 뒤의 두 값은 기간 집계입니다.
   *
   * @param unansweredInquiries 아직 답변하지 않은 문의 수
   * @param pendingWordRequests 승인도 거절도 되지 않은 단어 요청 수
   * @param newUsers 기간 내 가입한 사용자 수
   * @param adminActions 기간 내 기록된 관리자 작업 수
   */
  public record OperationsView(
      long unansweredInquiries, long pendingWordRequests, long newUsers, long adminActions) {}

  /**
   * 요청이 많은 API Key입니다.
   *
   * @param apiKeyId API Key 식별자. 키 원장에서 찾지 못하면 null입니다.
   * @param name 키 이름. 찾지 못하면 null입니다.
   * @param keyHint 표시용 힌트. 찾지 못하면 null입니다.
   * @param ownerName 소유자 표시 이름. 소유자가 연결되지 않았으면 null입니다.
   * @param totalRequests 기간 내 요청 수
   * @param detectedRequests 기간 내 검출된 요청 수
   * @param detectionRate 검출률(%). 소수점 첫째 자리까지입니다.
   * @param lastUsedAt 마지막 인증 시각. 관리자 키 목록과 같은 저장값을 그대로 내보냅니다. 기록이 없으면 null입니다.
   */
  public record TopApiKeyView(
      UUID apiKeyId,
      String name,
      String keyHint,
      String ownerName,
      long totalRequests,
      long detectedRequests,
      double detectionRate,
      LocalDateTime lastUsedAt) {}
}
