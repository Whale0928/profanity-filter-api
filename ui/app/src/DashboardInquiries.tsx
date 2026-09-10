import { ArrowUpRight, ChatCircleText, Plus, X } from "@phosphor-icons/react";
import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react";

import {
  createInquiry,
  getMyInquiry,
  inquiryStatusLabel,
  listMyInquiries,
  wordRequestStatusLabel,
  wordRequestTypeLabel,
  INQUIRY_TYPE_LABELS,
  WORD_SEVERITY_LABELS,
  type InquiryDetail,
  type InquirySummary,
  type InquiryType,
  type NewWordRequestType,
  type WordSeverity,
} from "./inquiries";

export default function DashboardInquiries({ accessToken }: { accessToken: string }) {
  const [items, setItems] = useState<InquirySummary[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [creating, setCreating] = useState(false);
  const [detailId, setDetailId] = useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    listMyInquiries(accessToken, page, controller.signal)
      .then((result) => { setItems(result.items); setHasNext(result.hasNext); })
      .catch(() => { if (!controller.signal.aborted) setError("문의 목록을 불러오지 못했습니다."); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [accessToken, page, retry]);

  function handleCreated() {
    setCreating(false);
    setPage(0);
    setRetry((n) => n + 1);
  }

  return (
    <section className="dashboard-inquiries">
      <header className="keys-heading">
        <div>
          <p className="eyebrow">Support</p>
          <h2>내 문의</h2>
          <p>작성한 문의와 처리 상태를 확인하세요.</p>
        </div>
        <button className="primary-action" onClick={() => setCreating(true)} type="button">
          <Plus size={18} /> 새 문의
        </button>
      </header>

      {error ? <div className="keys-error" role="alert">{error}<button onClick={() => setRetry((n) => n + 1)} type="button">다시 시도</button></div> : null}
      {loading ? <div className="keys-state" role="status">문의 목록을 확인하고 있습니다.</div> : null}
      {!loading && !error && items.length === 0 ? (
        <div className="keys-empty">
          <span><ChatCircleText size={28} /></span>
          <h2>아직 작성한 문의가 없습니다.</h2>
          <p>단어 요청이나 서비스 이용 관련 문의를 남겨 주세요.</p>
          <button onClick={() => setCreating(true)} type="button">첫 문의 작성하기</button>
        </div>
      ) : null}

      {items.length > 0 ? (
        <div className="key-list">
          {items.map((item) => (
            <article className="key-row" key={item.id}>
              <div className="key-primary">
                <div><h2>{item.title}</h2><span className="key-status"><i />{inquiryStatusLabel(item.status)}</span></div>
                <p>{INQUIRY_TYPE_LABELS[item.type as InquiryType] ?? item.type}</p>
              </div>
              <dl>
                <div><dt>접수일</dt><dd>{formatDate(item.createdAt)}</dd></div>
                <div><dt>완료일</dt><dd>{item.resolvedAt ? formatDate(item.resolvedAt) : "-"}</dd></div>
              </dl>
              <div className="key-actions">
                <button onClick={() => setDetailId(item.id)} type="button"><ArrowUpRight size={17} /> 상세보기</button>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      {!loading && !error && (page > 0 || hasNext) ? (
        <nav aria-label="문의 페이지" className="updates-pagination">
          <button disabled={!page} onClick={() => setPage((n) => n - 1)} type="button">이전</button>
          <span>{page + 1}</span>
          <button disabled={!hasNext} onClick={() => setPage((n) => n + 1)} type="button">다음</button>
        </nav>
      ) : null}

      {creating ? <CreateInquiryDialog accessToken={accessToken} onClose={() => setCreating(false)} onCreated={handleCreated} /> : null}
      {detailId ? <InquiryDetailDialog accessToken={accessToken} id={detailId} onClose={() => setDetailId(null)} /> : null}
    </section>
  );
}

function CreateInquiryDialog({
  accessToken,
  onClose,
  onCreated,
}: {
  accessToken: string;
  onClose: () => void;
  onCreated: () => void;
}) {
  const [type, setType] = useState<InquiryType>("GENERAL");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [word, setWord] = useState("");
  const [requestType, setRequestType] = useState<NewWordRequestType>("ADD");
  const [severity, setSeverity] = useState<WordSeverity>("MEDIUM");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await createInquiry(accessToken, type === "WORD_REQUEST" ? { content, requestType, severity, title, type, word } : { content, title, type });
      onCreated();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "문의를 등록하지 못했습니다.");
      setSubmitting(false);
    }
  }

  return (
    <Modal label="새 문의 작성" onClose={onClose}>
      <form className="key-form" onSubmit={submit}>
        <div><p className="eyebrow">New inquiry</p><h2>새 문의</h2><p>문의 유형에 맞춰 필요한 내용을 입력하세요.</p></div>
        <label>문의 유형
          <select onChange={(event) => setType(event.target.value as InquiryType)} value={type}>
            {Object.entries(INQUIRY_TYPE_LABELS).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </select>
        </label>
        <label>제목<input autoFocus maxLength={160} onChange={(event) => setTitle(event.target.value)} required value={title} /></label>
        <label>내용<textarea maxLength={2000} onChange={(event) => setContent(event.target.value)} required value={content} /></label>
        {type === "WORD_REQUEST" ? (
          <>
            <label>대상 표현<input maxLength={80} onChange={(event) => setWord(event.target.value)} required value={word} /></label>
            <label>요청 유형
              <select onChange={(event) => setRequestType(event.target.value as NewWordRequestType)} value={requestType}>
                <option value="ADD">추가</option>
                <option value="REMOVE">제외</option>
                <option value="MODIFY">수정</option>
              </select>
            </label>
            <label>심각도
              <select onChange={(event) => setSeverity(event.target.value as WordSeverity)} value={severity}>
                {Object.entries(WORD_SEVERITY_LABELS).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
              </select>
            </label>
            {requestType === "MODIFY" ? <p className="form-hint">수정 요청은 대상 표현을 어떻게 바꿀지 정하는 항목이 아직 없어, 운영팀이 문의 내용을 직접 확인한 뒤 처리합니다.</p> : null}
          </>
        ) : null}
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <div className="dialog-actions"><button onClick={onClose} type="button">취소</button><button className="primary-action" disabled={submitting} type="submit">{submitting ? "등록 중" : "문의 등록"}</button></div>
      </form>
    </Modal>
  );
}

function InquiryDetailDialog({ accessToken, id, onClose }: { accessToken: string; id: number; onClose: () => void }) {
  const [detail, setDetail] = useState<InquiryDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getMyInquiry(accessToken, id, controller.signal)
      .then(setDetail)
      .catch(() => { if (!controller.signal.aborted) setError("문의를 불러오지 못했습니다."); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [accessToken, id, retry]);

  return (
    <Modal label="문의 상세" onClose={onClose}>
      {loading ? <p className="keys-state" role="status">문의를 불러오고 있습니다.</p> : null}
      {!loading && error ? <div className="keys-error" role="alert">{error}<button onClick={() => setRetry((n) => n + 1)} type="button">다시 시도</button></div> : null}
      {!loading && !error && detail ? (
        <div className="inquiry-detail">
          <p className="eyebrow">{INQUIRY_TYPE_LABELS[detail.type as InquiryType] ?? detail.type}</p>
          <h2>{detail.title}</h2>
          <span className="key-status"><i />{inquiryStatusLabel(detail.status)}</span>
          <p>{detail.content}</p>
          {detail.wordRequest ? (
            <dl>
              <div><dt>대상 표현</dt><dd>{detail.wordRequest.word}</dd></div>
              <div><dt>요청 유형</dt><dd>{wordRequestTypeLabel(detail.wordRequest.requestType)}</dd></div>
              <div><dt>처리 상태</dt><dd>{wordRequestStatusLabel(detail.wordRequest.status)}</dd></div>
            </dl>
          ) : null}
          <h3>답변</h3>
          {detail.replies.length > 0 ? (
            <ul className="inquiry-replies">
              {detail.replies.map((reply) => <li key={reply.id}><p>{reply.content}</p><span>{reply.authorName} · {formatDate(reply.createdAt)}</span></li>)}
            </ul>
          ) : <p className="form-hint">아직 등록된 답변이 없습니다.</p>}
        </div>
      ) : null}
    </Modal>
  );
}

function Modal({ children, label, onClose }: { children: ReactNode; label: string; onClose: () => void }) {
  const modalRef = useRef<HTMLElement>(null);
  useEffect(() => {
    const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const header = document.querySelector<HTMLElement>(".global-header");
    header?.setAttribute("inert", "");
    document.body.style.overflow = "hidden";

    const focusable = () => Array.from(modalRef.current?.querySelectorAll<HTMLElement>("button:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled)") ?? []);
    focusable()[0]?.focus();

    const handleKeydown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
        return;
      }
      if (event.key !== "Tab") return;
      const elements = focusable();
      if (elements.length === 0) return;
      const first = elements[0];
      const last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    window.addEventListener("keydown", handleKeydown);
    return () => {
      window.removeEventListener("keydown", handleKeydown);
      header?.removeAttribute("inert");
      document.body.style.overflow = "";
      previouslyFocused?.focus();
    };
  }, []);
  return (
    <div className="dialog-backdrop" onMouseDown={onClose} role="presentation">
      <section aria-label={label} aria-modal="true" className="dialog key-dialog" onMouseDown={(event) => event.stopPropagation()} ref={modalRef} role="dialog">
        <button aria-label="닫기" className="dialog-close" onClick={onClose} type="button"><X size={20} /></button>
        {children}
      </section>
    </div>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium" }).format(new Date(value));
}
