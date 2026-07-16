package app.application.manage;

/**
 * @deprecated 기존 일일 리포트 수집은 중단을 검토 중이며 신규 기능에서 사용하지 않습니다.
 */
@Deprecated(forRemoval = true)
public interface ReportManagementService {
  /** 일일 리포트 생성 */
  int createDailyReport();
}
