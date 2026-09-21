package app.domain.record;

import java.time.LocalDate;

/**
 * 하루 단위 요청 집계입니다.
 *
 * @param date 서비스 시간대(Asia/Seoul) 기준 일자
 * @param totalRequests 그날 기록된 총 요청 수
 * @param detectedRequests 그날 비속어가 검출된 요청 수
 */
public record DailyRequestVolume(LocalDate date, long totalRequests, long detectedRequests) {}
