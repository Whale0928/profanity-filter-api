import { MagnifyingGlass, Plus, X } from "@phosphor-icons/react";
import { useEffect, useState } from "react";

import type { UserRole as AdminUserRole } from "./auth";
import { revokeAdminKey, listAdminKeys, type AdminKeyStatus, type AdminKeyView } from "./adminKeys";
import {
  decideWordRequest,
  getAdminInquiry,
  inquiryStatusLabel,
  listAdminInquiries,
  replyToInquiry,
  updateInquiryStatus,
  wordRequestStatusLabel,
  wordRequestTypeLabel,
  INQUIRY_TYPE_LABELS,
  type InquiryDetail,
  type InquirySummary,
  type InquiryStatus,
  type InquiryType,
} from "./inquiries";
import { listUsers, updateUserStatus, type UserView } from "./users";
import { createWord, listWords, updateWord, type WordUsed, type WordView, WORD_SOURCE_LABELS } from "./words";

type Menu = "dictionary" | "inquiries" | "users" | "keys";

export default function AdminPanels({ accessToken, currentUserId, menu }: { accessToken: string; currentUserId: string | null; menu: Menu }) {
  if (menu === "dictionary") return <WordsPanel accessToken={accessToken} />;
  if (menu === "inquiries") return <InquiriesPanel accessToken={accessToken} />;
  if (menu === "users") return <UsersPanel accessToken={accessToken} currentUserId={currentUserId} />;
  return <KeysPanel accessToken={accessToken} />;
}

function useDebounced<T>(value: T, delay: number) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

function Pagination({ hasNext, onPage, page }: { hasNext: boolean; onPage: (page: number) => void; page: number }) {
  if (page === 0 && !hasNext) return null;
  return (
    <nav aria-label="페이지" className="updates-pagination">
      <button disabled={!page} onClick={() => onPage(page - 1)} type="button">이전</button>
      <span>{page + 1}</span>
      <button disabled={!hasNext} onClick={() => onPage(page + 1)} type="button">다음</button>
    </nav>
  );
}

// ---------- 필터 사전 ----------

function WordsPanel({ accessToken }: { accessToken: string }) {
  const [query, setQuery] = useState("");
  const debouncedQuery = useDebounced(query, 300);
  const [isUsed, setIsUsed] = useState<WordUsed | "">("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<WordView[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [adding, setAdding] = useState(false);
  const [selected, setSelected] = useState<WordView | null>(null);
  const [word, setWord] = useState("");
  const [message, setMessage] = useState("");
  const [dialogError, setDialogError] = useState("");
  const [pending, setPending] = useState(false);

  useEffect(() => { setPage(0); }, [debouncedQuery, isUsed]);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError("");
    listWords(accessToken, debouncedQuery, isUsed, page, controller.signal)
      .then(result => { setItems(result.items); setHasNext(result.hasNext); })
      .catch(() => { if (!controller.signal.aborted) setError("사전 목록을 불러오지 못했습니다."); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [accessToken, debouncedQuery, isUsed, page, retry]);

  function refresh(text: string) { setRetry(n => n + 1); setMessage(text); }
  function openAdd() { setAdding(true); setWord(""); setDialogError(""); }
  function openEdit(row: WordView) { setSelected(row); setWord(row.word); setDialogError(""); }
  function close() { setAdding(false); setSelected(null); setPending(false); }

  async function submitCreate() {
    if (!word.trim()) return;
    setPending(true); setDialogError("");
    try { await createWord(accessToken, word.trim()); close(); refresh("사전에 표현을 추가했습니다."); }
    catch (requestError) { setDialogError(requestError instanceof Error ? requestError.message : "표현을 추가하지 못했습니다."); setPending(false); }
  }
  async function submitEdit() {
    if (!selected || !word.trim()) return;
    setPending(true); setDialogError("");
    try { await updateWord(accessToken, selected.id, word.trim(), selected.isUsed); close(); refresh("표현을 수정했습니다."); }
    catch (requestError) { setDialogError(requestError instanceof Error ? requestError.message : "표현을 수정하지 못했습니다."); setPending(false); }
  }
  async function toggleUsed() {
    if (!selected) return;
    setPending(true); setDialogError("");
    try { await updateWord(accessToken, selected.id, selected.word, selected.isUsed === "Y" ? "N" : "Y"); close(); refresh("적용 상태를 변경했습니다."); }
    catch (requestError) { setDialogError(requestError instanceof Error ? requestError.message : "적용 상태를 변경하지 못했습니다."); setPending(false); }
  }

  return (
    <div className="console-panel mock-panel">
      <header className="admin-header"><p>검출할 표현과 사전 적용 상태를 관리합니다.</p><button className="compact-button filled" onClick={openAdd} type="button"><Plus size={14} />표현 추가</button></header>
      {message ? <div className="admin-feedback" role="status">{message}<button aria-label="알림 닫기" onClick={() => setMessage("")} type="button"><X size={14} /></button></div> : null}
      <div className="admin-list-toolbar">
        <div aria-label="적용 상태 필터" className="admin-status-tabs" role="group">
          {([["", "전체"], ["Y", "사용 중"], ["N", "사용 안 함"]] as const).map(([value, label]) => <button aria-pressed={isUsed === value} key={value} onClick={() => setIsUsed(value)} type="button">{label}</button>)}
        </div>
        <label className="updates-search"><MagnifyingGlass size={14} /><input aria-label="표현 검색" onChange={event => setQuery(event.target.value)} placeholder="표현 검색" value={query} /></label>
      </div>
      {error ? <div className="keys-error" role="alert">{error}<button onClick={() => setRetry(n => n + 1)} type="button">다시 시도</button></div> : null}
      {loading ? <p className="updates-empty" role="status">사전 목록을 불러오고 있습니다.</p> : <>
        <div className="mock-table-scroll"><table className="mock-table"><thead><tr><th>표현</th><th>출처</th><th>상태</th><th>수정일</th><th><span className="sr-only">작업</span></th></tr></thead><tbody>
          {items.map(row => <tr key={row.id}>
            <td><button className="mock-title" onClick={() => openEdit(row)} type="button">{row.word}</button></td>
            <td>{WORD_SOURCE_LABELS[row.source]}</td>
            <td><span className={`mock-state ${row.isUsed === "Y" ? "is-active" : ""}`}>{row.isUsed === "Y" ? "사용 중" : "사용 안 함"}</span></td>
            <td><time>{row.updatedAt ? formatDate(row.updatedAt) : "-"}</time></td>
            <td><button aria-label={`${row.word} 상세`} className="mock-action" onClick={() => openEdit(row)} type="button">상세</button></td>
          </tr>)}
        </tbody></table>{!items.length ? <p className="updates-empty">조건에 맞는 표현이 없습니다.</p> : null}</div>
        <p className="admin-table-note">{items.length}개 표시 중</p>
        <Pagination hasNext={hasNext} onPage={setPage} page={page} />
      </>}
      {(adding || selected) ? <dialog aria-labelledby="word-dialog-title" className="admin-dialog mock-dialog" onCancel={close} ref={node => { if (node && !node.open) node.showModal(); }}>
        <header><h2 id="word-dialog-title">{adding ? "표현 추가" : "표현 상세"}</h2><button aria-label="닫기" className="mock-action" onClick={close} type="button"><X size={16} /></button></header>
        <label className="mock-field">표현<input autoFocus maxLength={80} onChange={event => setWord(event.target.value)} value={word} /></label>
        {selected ? <p className="mock-hint">출처: {WORD_SOURCE_LABELS[selected.source]} · 현재 상태: {selected.isUsed === "Y" ? "사용 중" : "사용 안 함"}</p> : null}
        {dialogError ? <p className="form-error" role="alert">{dialogError}</p> : null}
        <div className="mock-dialog-actions">
          <button className="compact-button" onClick={close} type="button">닫기</button>
          <button className="compact-button filled" disabled={!word.trim() || pending} onClick={() => void (adding ? submitCreate() : submitEdit())} type="button">저장</button>
          {selected ? <button className="compact-button" disabled={pending} onClick={() => void toggleUsed()} type="button">{selected.isUsed === "Y" ? "사용 중지" : "사용하기"}</button> : null}
        </div>
      </dialog> : null}
    </div>
  );
}

// ---------- 문의 ----------

function InquiriesPanel({ accessToken }: { accessToken: string }) {
  const [type, setType] = useState<InquiryType | "">("");
  const [status, setStatus] = useState<string>("");
  const [query, setQuery] = useState("");
  const debouncedQuery = useDebounced(query, 300);
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<InquirySummary[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [message, setMessage] = useState("");
  const [selectedId, setSelectedId] = useState<number | null>(null);

  useEffect(() => { setPage(0); }, [debouncedQuery, type, status]);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError("");
    listAdminInquiries(accessToken, type, status, debouncedQuery, page, controller.signal)
      .then(result => { setItems(result.items); setHasNext(result.hasNext); })
      .catch(() => { if (!controller.signal.aborted) setError("문의 목록을 불러오지 못했습니다."); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [accessToken, type, status, debouncedQuery, page, retry]);

  return (
    <div className="console-panel mock-panel">
      <header className="admin-header"><p>단어 요청과 서비스 문의를 확인하고 답변합니다.</p></header>
      {message ? <div className="admin-feedback" role="status">{message}<button aria-label="알림 닫기" onClick={() => setMessage("")} type="button"><X size={14} /></button></div> : null}
      <div className="admin-list-toolbar">
        <div aria-label="문의 유형 필터" className="admin-status-tabs" role="group">
          {([["", "전체 유형"], ["GENERAL", "일반 문의"], ["WORD_REQUEST", "단어 요청"]] as const).map(([value, label]) => <button aria-pressed={type === value} key={value} onClick={() => setType(value)} type="button">{label}</button>)}
        </div>
        <div aria-label="문의 상태 필터" className="admin-status-tabs" role="group">
          {([["", "전체 상태"], ["RECEIVED", "접수"], ["IN_PROGRESS", "처리 중"], ["RESOLVED", "완료"]] as const).map(([value, label]) => <button aria-pressed={status === value} key={value} onClick={() => setStatus(value)} type="button">{label}</button>)}
        </div>
        <label className="updates-search"><MagnifyingGlass size={14} /><input aria-label="문의 검색" onChange={event => setQuery(event.target.value)} placeholder="제목 검색" value={query} /></label>
      </div>
      {error ? <div className="keys-error" role="alert">{error}<button onClick={() => setRetry(n => n + 1)} type="button">다시 시도</button></div> : null}
      {loading ? <p className="updates-empty" role="status">문의 목록을 불러오고 있습니다.</p> : <>
        <div className="mock-table-scroll"><table className="mock-table"><thead><tr><th>문의 내용</th><th>유형</th><th>상태</th><th>접수일</th><th><span className="sr-only">작업</span></th></tr></thead><tbody>
          {items.map(row => <tr key={row.id}>
            <td><button className="mock-title" onClick={() => setSelectedId(row.id)} type="button">{row.title}</button><small>{row.requesterName ?? "탈퇴한 사용자"} · {row.requesterEmail ?? "-"}</small></td>
            <td>{INQUIRY_TYPE_LABELS[row.type as InquiryType] ?? row.type}</td>
            <td><span className={`mock-state ${row.status === "RESOLVED" ? "is-active" : ""}`}>{inquiryStatusLabel(row.status)}</span></td>
            <td><time>{formatDate(row.createdAt)}</time></td>
            <td><button aria-label={`${row.title} 상세`} className="mock-action" onClick={() => setSelectedId(row.id)} type="button">상세</button></td>
          </tr>)}
        </tbody></table>{!items.length ? <p className="updates-empty">조건에 맞는 문의가 없습니다.</p> : null}</div>
        <p className="admin-table-note">{items.length}개 표시 중</p>
        <Pagination hasNext={hasNext} onPage={setPage} page={page} />
      </>}
      {selectedId ? <InquiryDetailDialog accessToken={accessToken} id={selectedId} onChanged={text => { setMessage(text); setRetry(n => n + 1); }} onClose={() => setSelectedId(null)} /> : null}
    </div>
  );
}

function InquiryDetailDialog({ accessToken, id, onChanged, onClose }: { accessToken: string; id: number; onChanged: (message: string) => void; onClose: () => void }) {
  const [detail, setDetail] = useState<InquiryDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [reply, setReply] = useState("");
  const [resolveOnReply, setResolveOnReply] = useState(true);
  const [nextStatus, setNextStatus] = useState<InquiryStatus>("RECEIVED");
  const [decisionReason, setDecisionReason] = useState("");
  const [pending, setPending] = useState(false);
  const [actionError, setActionError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError("");
    getAdminInquiry(accessToken, id, controller.signal)
      .then(result => { setDetail(result); setNextStatus((result.status as InquiryStatus) || "RECEIVED"); })
      .catch(() => { if (!controller.signal.aborted) setError("문의를 불러오지 못했습니다."); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [accessToken, id, retry]);

  async function submitReply() {
    if (!reply.trim()) return;
    setPending(true); setActionError("");
    try { const updated = await replyToInquiry(accessToken, id, reply.trim(), resolveOnReply); setDetail(updated); setReply(""); onChanged("답변을 등록했습니다."); }
    catch (requestError) { setActionError(requestError instanceof Error ? requestError.message : "답변을 등록하지 못했습니다."); }
    finally { setPending(false); }
  }
  async function submitStatus() {
    setPending(true); setActionError("");
    try { const updated = await updateInquiryStatus(accessToken, id, nextStatus); setDetail(updated); onChanged("문의 상태를 변경했습니다."); }
    catch (requestError) { setActionError(requestError instanceof Error ? requestError.message : "상태를 변경하지 못했습니다."); }
    finally { setPending(false); }
  }
  async function submitDecision(decision: "APPROVE" | "REJECT") {
    setPending(true); setActionError("");
    try { const updated = await decideWordRequest(accessToken, id, decision, decisionReason.trim()); setDetail(updated); setDecisionReason(""); onChanged(decision === "APPROVE" ? "단어 요청을 승인했습니다." : "단어 요청을 거절했습니다."); }
    catch (requestError) { setActionError(requestError instanceof Error ? requestError.message : "단어 요청을 처리하지 못했습니다."); }
    finally { setPending(false); }
  }

  const wordRequest = detail?.wordRequest ?? null;
  const isModify = wordRequest?.requestType === "MODIFY";
  const decidable = wordRequest?.status === "REQUEST";

  return (
    <dialog aria-labelledby="inquiry-dialog-title" className="admin-dialog mock-dialog" onCancel={onClose} ref={node => { if (node && !node.open) node.showModal(); }}>
      <header><h2 id="inquiry-dialog-title">문의 상세</h2><button aria-label="닫기" className="mock-action" onClick={onClose} type="button"><X size={16} /></button></header>
      {loading ? <p className="updates-empty" role="status">문의를 불러오고 있습니다.</p> : null}
      {!loading && error ? <div className="keys-error" role="alert">{error}<button onClick={() => setRetry(n => n + 1)} type="button">다시 시도</button></div> : null}
      {!loading && !error && detail ? <>
        <h3>{detail.title}</h3>
        <p className="mock-inquiry-body">{detail.content}</p>
        <dl><dt>유형</dt><dd>{INQUIRY_TYPE_LABELS[detail.type as InquiryType] ?? detail.type}</dd><dt>요청자</dt><dd>{detail.requesterName ?? "탈퇴한 사용자"} ({detail.requesterEmail ?? "-"})</dd><dt>상태</dt><dd>{inquiryStatusLabel(detail.status)}</dd></dl>

        {wordRequest ? <div className="mock-word-request">
          <h3>단어 요청 정보</h3>
          <dl><dt>대상 표현</dt><dd>{wordRequest.word}</dd><dt>요청 유형</dt><dd>{wordRequestTypeLabel(wordRequest.requestType)}</dd><dt>사유</dt><dd>{wordRequest.reason}</dd><dt>심각도</dt><dd>{wordRequest.severity}</dd><dt>처리 상태</dt><dd>{wordRequestStatusLabel(wordRequest.status)}</dd></dl>
          {isModify ? <p className="mock-hint">기존 계약에는 수정 요청이 바꿀 새 표현 정보가 없어 이 요청은 승인할 수 없습니다. 거절하거나 사용자에게 새 문의로 다시 요청하도록 안내하세요.</p> : null}
          {decidable ? <label className="mock-field">처리 사유 <small>선택</small><input maxLength={500} onChange={event => setDecisionReason(event.target.value)} value={decisionReason} /></label> : null}
          {decidable ? <div className="mock-dialog-actions">
            <button className="compact-button filled" disabled={pending || isModify} onClick={() => void submitDecision("APPROVE")} type="button">승인</button>
            <button className="compact-button" disabled={pending} onClick={() => void submitDecision("REJECT")} type="button">거절</button>
          </div> : <p className="mock-hint">이미 처리된 요청입니다: {wordRequestStatusLabel(wordRequest.status)}</p>}
        </div> : null}

        <h3>상태 변경</h3>
        <div className="mock-field-row"><select onChange={event => setNextStatus(event.target.value as InquiryStatus)} value={nextStatus}><option value="RECEIVED">접수</option><option value="IN_PROGRESS">처리 중</option><option value="RESOLVED">완료</option></select><button className="compact-button" disabled={pending || nextStatus === detail.status} onClick={() => void submitStatus()} type="button">상태 변경</button></div>

        <h3>답변</h3>
        {detail.replies.length ? <ul className="mock-replies">{detail.replies.map(item => <li key={item.id}><p>{item.content}</p><span>{item.authorName} · {formatDate(item.createdAt)}</span></li>)}</ul> : <p className="mock-hint">아직 등록된 답변이 없습니다.</p>}
        <label className="mock-field">답변 작성<textarea onChange={event => setReply(event.target.value)} placeholder="답변을 작성하세요" value={reply} /></label>
        <label className="mock-checkbox"><input checked={resolveOnReply} onChange={event => setResolveOnReply(event.target.checked)} type="checkbox" />답변 등록 후 문의를 완료 처리합니다.</label>
        {actionError ? <p className="form-error" role="alert">{actionError}</p> : null}
        <div className="mock-dialog-actions"><button className="compact-button" onClick={onClose} type="button">닫기</button><button className="compact-button filled" disabled={!reply.trim() || pending} onClick={() => void submitReply()} type="button">답변 등록</button></div>
      </> : null}
    </dialog>
  );
}

// ---------- 사용자 관리 ----------

function UsersPanel({ accessToken, currentUserId }: { accessToken: string; currentUserId: string | null }) {
  const [query, setQuery] = useState("");
  const debouncedQuery = useDebounced(query, 300);
  const [role, setRole] = useState<AdminUserRole | "">("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<UserView[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [message, setMessage] = useState("");
  const [selected, setSelected] = useState<UserView | null>(null);
  const [pending, setPending] = useState(false);
  const [dialogError, setDialogError] = useState("");

  useEffect(() => { setPage(0); }, [debouncedQuery, role]);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError("");
    listUsers(accessToken, debouncedQuery, role, page, controller.signal)
      .then(result => { setItems(result.items); setHasNext(result.hasNext); })
      .catch(() => { if (!controller.signal.aborted) setError("사용자 목록을 불러오지 못했습니다."); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [accessToken, role, debouncedQuery, page, retry]);

  function protectedRow(row: UserView) { return row.role === "ADMIN" || row.id === currentUserId; }

  async function toggleStatus() {
    if (!selected) return;
    setPending(true); setDialogError("");
    try {
      await updateUserStatus(accessToken, selected.id, selected.status === "ACTIVE" ? "DISABLED" : "ACTIVE");
      setSelected(null); setRetry(n => n + 1); setMessage("계정 상태를 변경했습니다.");
    } catch (requestError) { setDialogError(requestError instanceof Error ? requestError.message : "계정 상태를 변경하지 못했습니다."); }
    finally { setPending(false); }
  }

  return (
    <div className="console-panel mock-panel">
      <header className="admin-header"><p>가입한 사용자와 계정 상태를 확인합니다.</p></header>
      {message ? <div className="admin-feedback" role="status">{message}<button aria-label="알림 닫기" onClick={() => setMessage("")} type="button"><X size={14} /></button></div> : null}
      <div className="admin-list-toolbar">
        <div aria-label="역할 필터" className="admin-status-tabs" role="group">
          {([["", "전체"], ["ADMIN", "관리자"], ["CLIENT", "일반 사용자"]] as const).map(([value, label]) => <button aria-pressed={role === value} key={value} onClick={() => setRole(value)} type="button">{label}</button>)}
        </div>
        <label className="updates-search"><MagnifyingGlass size={14} /><input aria-label="사용자 검색" onChange={event => setQuery(event.target.value)} placeholder="이름 또는 이메일 검색" value={query} /></label>
      </div>
      {error ? <div className="keys-error" role="alert">{error}<button onClick={() => setRetry(n => n + 1)} type="button">다시 시도</button></div> : null}
      {loading ? <p className="updates-empty" role="status">사용자 목록을 불러오고 있습니다.</p> : <>
        <div className="mock-table-scroll"><table className="mock-table"><thead><tr><th>사용자</th><th>역할</th><th>상태</th><th>최근 접속</th><th><span className="sr-only">작업</span></th></tr></thead><tbody>
          {items.map(row => <tr key={row.id}>
            <td><button className="mock-title" onClick={() => setSelected(row)} type="button">{row.displayName}</button><small>{row.primaryEmail}</small></td>
            <td>{row.role === "ADMIN" ? "관리자" : "일반 사용자"}</td>
            <td><span className={`mock-state ${row.status === "ACTIVE" ? "is-active" : ""}`}>{row.status === "ACTIVE" ? "활성" : "비활성"}</span></td>
            <td><time>{row.lastLoginAt ? formatDate(row.lastLoginAt) : "-"}</time></td>
            <td><button aria-label={`${row.displayName} 상세`} className="mock-action" onClick={() => setSelected(row)} type="button">상세</button></td>
          </tr>)}
        </tbody></table>{!items.length ? <p className="updates-empty">조건에 맞는 사용자가 없습니다.</p> : null}</div>
        <p className="admin-table-note">{items.length}명 표시 중</p>
        <Pagination hasNext={hasNext} onPage={setPage} page={page} />
      </>}
      {selected ? <dialog aria-labelledby="user-dialog-title" className="admin-dialog mock-dialog" onCancel={() => setSelected(null)} ref={node => { if (node && !node.open) node.showModal(); }}>
        <header><h2 id="user-dialog-title">사용자 상세</h2><button aria-label="닫기" className="mock-action" onClick={() => setSelected(null)} type="button"><X size={16} /></button></header>
        <h3>{selected.displayName}</h3>
        <dl><dt>이메일</dt><dd>{selected.primaryEmail}</dd><dt>역할</dt><dd>{selected.role === "ADMIN" ? "관리자" : "일반 사용자"}</dd><dt>상태</dt><dd>{selected.status === "ACTIVE" ? "활성" : "비활성"}</dd><dt>가입일</dt><dd>{formatDate(selected.createdAt)}</dd></dl>
        {protectedRow(selected) ? <p className="mock-hint">자기 계정과 관리자 계정의 상태는 이 화면에서 변경할 수 없습니다.</p> : null}
        {dialogError ? <p className="form-error" role="alert">{dialogError}</p> : null}
        <div className="mock-dialog-actions">
          <button className="compact-button" onClick={() => setSelected(null)} type="button">닫기</button>
          <button className="compact-button" disabled={pending || protectedRow(selected)} onClick={() => void toggleStatus()} type="button">{selected.status === "ACTIVE" ? "계정 비활성화" : "계정 활성화"}</button>
        </div>
      </dialog> : null}
    </div>
  );
}

// ---------- API Key 관리 ----------

function KeysPanel({ accessToken }: { accessToken: string }) {
  const [query, setQuery] = useState("");
  const debouncedQuery = useDebounced(query, 300);
  const [status, setStatus] = useState<AdminKeyStatus | "">("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<AdminKeyView[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [message, setMessage] = useState("");
  const [revoking, setRevoking] = useState<AdminKeyView | null>(null);
  const [reason, setReason] = useState("");
  const [pending, setPending] = useState(false);
  const [dialogError, setDialogError] = useState("");

  useEffect(() => { setPage(0); }, [debouncedQuery, status]);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError("");
    listAdminKeys(accessToken, debouncedQuery, status, page, controller.signal)
      .then(result => { setItems(result.items); setHasNext(result.hasNext); })
      .catch(() => { if (!controller.signal.aborted) setError("API Key 목록을 불러오지 못했습니다."); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [accessToken, status, debouncedQuery, page, retry]);

  function openRevoke(row: AdminKeyView) { setRevoking(row); setReason(""); setDialogError(""); }

  async function submitRevoke() {
    if (!revoking) return;
    setPending(true); setDialogError("");
    try { await revokeAdminKey(accessToken, revoking.id, reason.trim()); setRevoking(null); setRetry(n => n + 1); setMessage("API Key를 만료했습니다."); }
    catch (requestError) { setDialogError(requestError instanceof Error ? requestError.message : "API Key를 만료하지 못했습니다."); }
    finally { setPending(false); }
  }

  return (
    <div className="console-panel mock-panel">
      <header className="admin-header"><p>발급된 키의 소유자와 사용 상태를 확인합니다.</p></header>
      {message ? <div className="admin-feedback" role="status">{message}<button aria-label="알림 닫기" onClick={() => setMessage("")} type="button"><X size={14} /></button></div> : null}
      <div className="admin-list-toolbar">
        <div aria-label="키 상태 필터" className="admin-status-tabs" role="group">
          {([["", "전체"], ["ACTIVE", "사용 중"], ["EXPIRED", "만료"]] as const).map(([value, label]) => <button aria-pressed={status === value} key={value} onClick={() => setStatus(value)} type="button">{label}</button>)}
        </div>
        <label className="updates-search"><MagnifyingGlass size={14} /><input aria-label="API Key 검색" onChange={event => setQuery(event.target.value)} placeholder="이름 또는 소유자 검색" value={query} /></label>
      </div>
      {error ? <div className="keys-error" role="alert">{error}<button onClick={() => setRetry(n => n + 1)} type="button">다시 시도</button></div> : null}
      {loading ? <p className="updates-empty" role="status">API Key 목록을 불러오고 있습니다.</p> : <>
        <div className="mock-table-scroll"><table className="mock-table"><thead><tr><th>API Key</th><th>소유자</th><th>상태</th><th>최근 사용</th><th><span className="sr-only">작업</span></th></tr></thead><tbody>
          {items.map(row => <tr key={row.id}>
            <td><button className="mock-title" onClick={() => openRevoke(row)} type="button">{row.name}</button><small>{row.keyHint}</small></td>
            <td>{row.ownerName} · {row.email}</td>
            <td><span className={`mock-state ${row.active ? "is-active" : ""}`}>{row.active ? "사용 중" : "만료"}</span></td>
            <td><time>{row.lastUsedAt ? formatDate(row.lastUsedAt) : "-"}</time></td>
            <td><button aria-label={`${row.name} 만료`} className="mock-action" disabled={!row.active} onClick={() => openRevoke(row)} type="button">만료</button></td>
          </tr>)}
        </tbody></table>{!items.length ? <p className="updates-empty">조건에 맞는 API Key가 없습니다.</p> : null}</div>
        <p className="admin-table-note">{items.length}개 표시 중</p>
        <Pagination hasNext={hasNext} onPage={setPage} page={page} />
      </>}
      {revoking ? <dialog aria-labelledby="key-dialog-title" className="admin-dialog mock-dialog" onCancel={() => setRevoking(null)} ref={node => { if (node && !node.open) node.showModal(); }}>
        <header><h2 id="key-dialog-title">API Key 만료</h2><button aria-label="닫기" className="mock-action" onClick={() => setRevoking(null)} type="button"><X size={16} /></button></header>
        <p className="mock-inquiry-body">{revoking.name} ({revoking.keyHint})의 현재 키는 만료 처리 즉시 사용할 수 없게 됩니다.</p>
        {revoking.active ? <label className="mock-field">만료 사유 <small>선택</small><input maxLength={500} onChange={event => setReason(event.target.value)} value={reason} /></label> : <p className="mock-hint">이미 만료된 키입니다.</p>}
        {dialogError ? <p className="form-error" role="alert">{dialogError}</p> : null}
        <div className="mock-dialog-actions"><button className="compact-button" onClick={() => setRevoking(null)} type="button">닫기</button>{revoking.active ? <button className="compact-button filled" disabled={pending} onClick={() => void submitRevoke()} type="button">만료 처리</button> : null}</div>
      </dialog> : null}
    </div>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
