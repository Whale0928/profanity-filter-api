import { adminRequest, buildQuery } from "./adminClient";

/** 서버가 허용하는 조회 기간입니다. 그 밖의 값은 서버가 거절합니다. */
export const STATISTICS_PERIODS = [7, 30, 90] as const;
export type StatisticsPeriod = typeof STATISTICS_PERIODS[number];

export type StatisticsSummary = {
  activeApiKeys: number;
  detectedRequests: number;
  detectionRate: number;
  totalApiKeys: number;
  totalRequests: number;
  totalWords: number;
  usedWords: number;
};

export type StatisticsDaily = { date: string; detectedRequests: number; totalRequests: number };
export type StatisticsMode = { mode: string; share: number; totalRequests: number };
export type StatisticsOperations = {
  adminActions: number;
  newUsers: number;
  pendingWordRequests: number;
  unansweredInquiries: number;
};
export type StatisticsTopApiKey = {
  apiKeyId: string | null;
  detectedRequests: number;
  detectionRate: number;
  keyHint: string | null;
  /** 시간대 없는 날짜시각 문자열입니다. */
  lastUsedAt: string | null;
  name: string | null;
  ownerName: string | null;
  totalRequests: number;
};

export type StatisticsView = {
  daily: StatisticsDaily[];
  modes: StatisticsMode[];
  operations: StatisticsOperations;
  period: { days: number; from: string; to: string };
  summary: StatisticsSummary;
  topApiKeys: StatisticsTopApiKey[];
};

export function loadStatistics(accessToken: string, days: StatisticsPeriod, signal?: AbortSignal) {
  const params = buildQuery({ days });
  return adminRequest<StatisticsView>(accessToken, `/api/v1/admin/statistics?${params}`, { signal });
}

/** 2026-09-21을 09.21로 줄입니다. 축 라벨은 연도 없이 보여 줍니다. */
export function shortDate(date: string) {
  return date.slice(5).replace("-", ".");
}

export function formatNumber(value: number) {
  return value.toLocaleString("ko-KR");
}

/**
 * 마지막 사용 시각을 표기합니다.
 *
 * 서버가 저장된 벽시계를 시간대 없이 그대로 내려 주므로 상대 시각으로 바꾸지 않고 값을 그대로 보여 줍니다.
 * 관리자 API Key 목록이 보여 주는 값과 같습니다.
 */
export function formatDateTime(value: string | null) {
  if (!value) return "기록 없음";
  const [date, time] = value.split("T");
  if (!date || !time) return value;
  return `${date.replace(/-/g, ".")} ${time.slice(0, 5)}`;
}
