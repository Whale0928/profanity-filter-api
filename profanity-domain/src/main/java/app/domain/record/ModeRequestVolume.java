package app.domain.record;

import app.core.data.constant.Mode;

/**
 * 필터링 모드별 요청 집계입니다.
 *
 * @param mode 필터링 모드
 * @param totalRequests 해당 모드로 기록된 요청 수
 */
public record ModeRequestVolume(Mode mode, long totalRequests) {}
