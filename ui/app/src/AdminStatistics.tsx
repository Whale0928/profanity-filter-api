import { useEffect, useMemo, useState } from "react";

import {
  formatDateTime,
  formatNumber,
  loadStatistics,
  shortDate,
  STATISTICS_PERIODS,
  type StatisticsPeriod,
  type StatisticsView,
} from "./statistics";

const MODE_DESCRIPTIONS: Record<string, string> = {
  FILTER: "검출 단어 마스킹",
  NORMAL: "전체 검출",
  QUICK: "첫 검출만",
};

/** 로그인하지 않은 미리보기에서 화면 구조를 보여 주기 위한 예시 값입니다. */
const PREVIEW: StatisticsView = {
  daily: [
    { date: "2026-09-15", detectedRequests: 498, totalRequests: 3412 },
    { date: "2026-09-16", detectedRequests: 552, totalRequests: 3880 },
    { date: "2026-09-17", detectedRequests: 611, totalRequests: 4102 },
    { date: "2026-09-18", detectedRequests: 524, totalRequests: 3664 },
    { date: "2026-09-19", detectedRequests: 702, totalRequests: 4470 },
    { date: "2026-09-20", detectedRequests: 448, totalRequests: 3285 },
    { date: "2026-09-21", detectedRequests: 577, totalRequests: 3668 },
  ],
  modes: [
    { mode: "NORMAL", share: 53.6, totalRequests: 14203 },
    { mode: "FILTER", share: 33.9, totalRequests: 8976 },
    { mode: "QUICK", share: 12.5, totalRequests: 3302 },
  ],
  operations: { adminActions: 31, newUsers: 9, pendingWordRequests: 12, unansweredInquiries: 7 },
  period: { days: 7, from: "2026-09-15", to: "2026-09-21" },
  summary: {
    activeApiKeys: 38,
    detectedRequests: 3912,
    detectionRate: 14.8,
    totalApiKeys: 52,
    totalRequests: 26481,
    totalWords: 1341,
    usedWords: 1284,
  },
  topApiKeys: [
    { apiKeyId: "k1", detectedRequests: 1514, detectionRate: 15.4, keyHint: "•••• A001", lastUsedAt: null, name: "커뮤니티 웹", ownerName: "개발자 A", totalRequests: 9842 },
    { apiKeyId: "k2", detectedRequests: 902, detectionRate: 14.3, keyHint: "•••• B002", lastUsedAt: null, name: "모바일 클라이언트", ownerName: "개발자 B", totalRequests: 6317 },
    { apiKeyId: "k3", detectedRequests: 741, detectionRate: 16.3, keyHint: "•••• C003", lastUsedAt: null, name: "게임 채팅", ownerName: "개발자 A", totalRequests: 4558 },
  ],
};

export default function AdminStatistics({ accessToken, preview }: { accessToken: string | null; preview: boolean }) {
  const [days, setDays] = useState<StatisticsPeriod>(7);
  const [view, setView] = useState<StatisticsView | null>(preview ? PREVIEW : null);
  const [loading, setLoading] = useState(!preview);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    if (preview || !accessToken) return;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    loadStatistics(accessToken, days, controller.signal)
      .then(loaded => { setView(loaded); setLoading(false); })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) return;
        setError(cause instanceof Error ? cause.message : "통계를 불러오지 못했습니다.");
        setLoading(false);
      });
    return () => controller.abort();
  }, [accessToken, preview, days, retry]);

  const maxDaily = useMemo(() => Math.max(1, ...(view?.daily ?? []).map(day => day.totalRequests)), [view]);
  const maxMode = useMemo(() => Math.max(1, ...(view?.modes ?? []).map(mode => mode.totalRequests)), [view]);

  return <div className="console-panel stats-panel">
    <header className="admin-header stats-header">
      <p>요청 기록을 기간별로 집계합니다. 캐시에 적중한 동일 요청은 기록되지 않아 집계에서 빠집니다.</p>
      <div className="stats-header-actions">
        {preview ? <span className="stats-badge">예시 데이터</span> : null}
        <div className="admin-status-tabs" role="group" aria-label="조회 기간">
          {STATISTICS_PERIODS.map(period => (
            <button key={period} type="button" aria-pressed={days === period} disabled={preview} onClick={() => setDays(period)}>{period}일</button>
          ))}
        </div>
      </div>
    </header>

    {error ? <div className="keys-error" role="alert">{error}<button type="button" onClick={() => setRetry(value => value + 1)}>다시 시도</button></div> : null}
    {loading ? <p className="updates-empty" role="status">통계를 불러오고 있습니다.</p> : null}

    {view && !loading ? <>
      <p className="stats-period">{view.period.from} ~ {view.period.to} · {view.period.days}일</p>

      <div className="stats-cards">
        <article className="stats-card">
          <span className="stats-card-label">총 요청</span>
          <strong className="stats-card-value">{formatNumber(view.summary.totalRequests)}</strong>
          <span className="stats-card-note">기록된 요청 수</span>
        </article>
        <article className="stats-card">
          <span className="stats-card-label">검출된 요청</span>
          <strong className="stats-card-value is-accent">{formatNumber(view.summary.detectedRequests)}<small>{view.summary.detectionRate}%</small></strong>
          <span className="stats-card-note">비속어가 검출된 요청</span>
        </article>
        <article className="stats-card">
          <span className="stats-card-label">사용 중 API Key</span>
          <strong className="stats-card-value">{formatNumber(view.summary.activeApiKeys)}<small>/ {formatNumber(view.summary.totalApiKeys)}</small></strong>
          <span className="stats-card-note">만료되지 않은 키 · 누적</span>
        </article>
        <article className="stats-card">
          <span className="stats-card-label">사전 단어</span>
          <strong className="stats-card-value">{formatNumber(view.summary.usedWords)}<small>/ {formatNumber(view.summary.totalWords)}</small></strong>
          <span className="stats-card-note">사용 중 / 전체 · 누적</span>
        </article>
      </div>

      <section className="stats-block">
        <div className="stats-block-head">
          <h2>일별 요청 추이<span>records</span></h2>
          <div className="stats-legend">
            <span><i className="is-detected" />검출됨</span>
            <span><i />검출 없음</span>
          </div>
        </div>
        <div className="stats-chart">
          {view.daily.map(day => {
            const total = Math.round((day.totalRequests / maxDaily) * 100);
            const detected = day.totalRequests ? Math.round((day.detectedRequests / day.totalRequests) * 100) : 0;
            return <div className="stats-bar-column" key={day.date}>
              <span className="stats-bar-total">{formatNumber(day.totalRequests)}</span>
              <div className="stats-bar-track">
                <div className="stats-bar" style={{ height: `${day.totalRequests ? Math.max(total, 1) : 0}%` }}>
                  <div className="stats-bar-detected" style={{ height: `${detected}%` }} />
                </div>
              </div>
              <span className="stats-bar-date">{shortDate(day.date)}</span>
              <span className="stats-bar-detected-value">{formatNumber(day.detectedRequests)}</span>
            </div>;
          })}
        </div>
        <p className="admin-table-note">막대 위 숫자는 그날 기록된 총 요청 수이고, 날짜 아래 숫자는 검출된 요청 수입니다.</p>
      </section>

      <div className="stats-columns">
        <section className="stats-block">
          <div className="stats-block-head"><h2>모드별 요청<span>records.mode</span></h2></div>
          <div className="stats-modes">
            {view.modes.map(mode => <div className="stats-mode" key={mode.mode}>
              <div className="stats-mode-head">
                <span className="stats-mode-name">{mode.mode}<small>{MODE_DESCRIPTIONS[mode.mode] ?? ""}</small></span>
                <span className="stats-mode-value">{formatNumber(mode.totalRequests)} · {mode.share}%</span>
              </div>
              <div className="stats-track"><div className="stats-track-fill" style={{ width: `${(mode.totalRequests / maxMode) * 100}%` }} /></div>
            </div>)}
          </div>
        </section>

        <section className="stats-block">
          <div className="stats-block-head"><h2>운영 현황</h2></div>
          <div className="stats-tiles">
            <div className="stats-tile"><span>미답변 문의</span><strong>{formatNumber(view.operations.unansweredInquiries)}</strong><small>inquiries</small></div>
            <div className="stats-tile"><span>대기 중 단어 요청</span><strong>{formatNumber(view.operations.pendingWordRequests)}</strong><small>word_management</small></div>
            <div className="stats-tile"><span>신규 가입</span><strong>{formatNumber(view.operations.newUsers)}</strong><small>users</small></div>
            <div className="stats-tile"><span>관리자 작업</span><strong>{formatNumber(view.operations.adminActions)}</strong><small>admin_audit_logs</small></div>
          </div>
        </section>
      </div>

      <section className="stats-block">
        <div className="stats-block-head"><h2>API Key 사용량 상위<span>records × api_keys</span></h2></div>
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>API Key</th><th>소유자</th><th className="is-numeric">요청</th><th className="is-numeric">검출</th><th className="is-numeric">검출률</th><th className="is-numeric">최근 사용</th></tr></thead>
            <tbody>
              {view.topApiKeys.map((key, index) => <tr key={key.apiKeyId ?? `unknown-${index}`}>
                <td>{key.name ?? <span className="stats-unknown">확인할 수 없는 키</span>}{key.keyHint ? <small className="stats-hint">{key.keyHint}</small> : null}</td>
                <td className="stats-owner">{key.ownerName ?? "연결된 소유자 없음"}</td>
                <td className="is-numeric">{formatNumber(key.totalRequests)}</td>
                <td className="is-numeric">{formatNumber(key.detectedRequests)}</td>
                <td className="is-numeric stats-rate">{key.detectionRate}%</td>
                <td className="is-numeric stats-last-used">{preview ? "-" : formatDateTime(key.lastUsedAt)}</td>
              </tr>)}
            </tbody>
          </table>
          {view.topApiKeys.length ? null : <p className="updates-empty">기간 안에 API Key로 들어온 요청이 없습니다.</p>}
        </div>
      </section>
    </> : null}
  </div>;
}
