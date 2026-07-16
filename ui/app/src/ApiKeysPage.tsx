import {
  ArrowClockwise,
  Check,
  Copy,
  Key,
  Plus,
  ShieldCheck,
  Trash,
  X,
} from "@phosphor-icons/react";
import { useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from "react";

import {
  expireApiKey,
  issueApiKey,
  listApiKeys,
  reissueApiKey,
  type ApiKeyView,
  type CreateApiKeyInput,
  type IssuedApiKey,
} from "./apiKeys";
import type { LoginUser } from "./auth";

type Action = { key: ApiKeyView; type: "expire" | "reissue" } | null;

export default function ApiKeysPage({
  accessToken,
  user,
}: {
  accessToken: string;
  user: LoginUser;
}) {
  const [keys, setKeys] = useState<ApiKeyView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [creating, setCreating] = useState(false);
  const [action, setAction] = useState<Action>(null);
  const [issued, setIssued] = useState<IssuedApiKey | null>(null);

  async function refresh() {
    setError("");
    try {
      setKeys(await listApiKeys(accessToken));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "API Key를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void refresh(); }, [accessToken]);

  async function handleCreated(result: IssuedApiKey) {
    setCreating(false);
    setIssued(result);
    await refresh();
  }

  async function handleAction() {
    if (!action) return;
    try {
      if (action.type === "reissue") {
        const result = await reissueApiKey(accessToken, action.key.id);
        setAction(null);
        setIssued(result);
      } else {
        await expireApiKey(accessToken, action.key.id);
        setAction(null);
      }
      await refresh();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "요청을 처리하지 못했습니다.");
      setAction(null);
    }
  }

  const activeCount = useMemo(() => keys.filter((key) => key.status === "ACTIVE").length, [keys]);

  return (
    <section className="keys-page page-width">
      <header className="keys-heading">
        <div>
          <p className="eyebrow">Credentials</p>
          <h1>API Key 관리</h1>
          <p>키 원문은 발급 직후 한 번만 확인할 수 있습니다.</p>
        </div>
        <button className="primary-action" onClick={() => setCreating(true)} type="button">
          <Plus size={18} /> 새 API Key
        </button>
      </header>

      <div aria-label="API Key 요약" className="keys-summary">
        <span><b>{activeCount}</b> 활성</span>
        <span><b>{keys.length - activeCount}</b> 만료</span>
        <span><ShieldCheck size={18} /> SSO 이메일 고정</span>
      </div>

      {error ? <div className="keys-error" role="alert">{error}<button onClick={() => void refresh()} type="button">다시 시도</button></div> : null}
      {loading ? <div className="keys-state" role="status">API Key를 확인하고 있습니다.</div> : null}
      {!loading && !error && keys.length === 0 ? (
        <div className="keys-empty">
          <span><Key size={28} /></span>
          <h2>아직 발급한 API Key가 없습니다.</h2>
          <p>용도별로 키를 나누면 재발행과 만료 처리가 쉬워집니다.</p>
          <button onClick={() => setCreating(true)} type="button">첫 API Key 만들기</button>
        </div>
      ) : null}

      {keys.length > 0 ? (
        <div className="key-list">
          {keys.map((apiKey) => (
            <article className="key-row" key={apiKey.id}>
              <div className="key-primary">
                <div><h2>{apiKey.name}</h2><Status status={apiKey.status} /></div>
                <code>{apiKey.keyHint}</code>
                <p>{apiKey.note || apiKey.issuerInfo}</p>
              </div>
              <dl>
                <div><dt>이메일</dt><dd>{apiKey.email}</dd></div>
                <div><dt>발급일</dt><dd>{formatDate(apiKey.issuedAt)}</dd></div>
                <div><dt>요청 수</dt><dd>{apiKey.requestCount.toLocaleString("ko-KR")}</dd></div>
              </dl>
              <div className="key-actions">
                <button disabled={apiKey.status === "EXPIRED"} onClick={() => setAction({ key: apiKey, type: "reissue" })} type="button">
                  <ArrowClockwise size={17} /> 재발행
                </button>
                <button className="danger-text" disabled={apiKey.status === "EXPIRED"} onClick={() => setAction({ key: apiKey, type: "expire" })} type="button">
                  <Trash size={17} /> 만료
                </button>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      {creating ? <CreateKeyDialog email={user.email} onClose={() => setCreating(false)} onCreated={handleCreated} token={accessToken} /> : null}
      {action ? <ConfirmActionDialog action={action} onClose={() => setAction(null)} onConfirm={handleAction} /> : null}
      {issued ? <IssuedKeyDialog issued={issued} onClose={() => setIssued(null)} /> : null}
    </section>
  );
}

function Status({ status }: { status: ApiKeyView["status"] }) {
  return <span className={`key-status key-status-${status.toLowerCase()}`}><i />{status === "ACTIVE" ? "활성" : "만료"}</span>;
}

function CreateKeyDialog({ email, onClose, onCreated, token }: { email: string; onClose: () => void; onCreated: (issued: IssuedApiKey) => void; token: string }) {
  const [form, setForm] = useState<CreateApiKeyInput>({ issuerInfo: "", name: "", note: "" });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await onCreated(await issueApiKey(token, form));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "API Key를 발급하지 못했습니다.");
      setSubmitting(false);
    }
  }

  return (
    <Modal label="새 API Key 발급" onClose={onClose}>
      <form className="key-form" onSubmit={submit}>
        <div><p className="eyebrow">New credential</p><h2>새 API Key</h2><p>환경이나 용도를 알아볼 수 있는 이름을 사용하세요.</p></div>
        <label>이름<input autoFocus maxLength={100} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="예: 운영 서버" required value={form.name} /></label>
        <label>SSO 이메일<input disabled value={email} /></label>
        <label>발급자 정보<input maxLength={255} onChange={(event) => setForm({ ...form, issuerInfo: event.target.value })} placeholder="예: production-api" required value={form.issuerInfo} /></label>
        <label>메모 <small>선택</small><textarea maxLength={255} onChange={(event) => setForm({ ...form, note: event.target.value })} placeholder="사용 위치나 담당자를 기록하세요." value={form.note} /></label>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <div className="dialog-actions"><button onClick={onClose} type="button">취소</button><button className="primary-action" disabled={submitting} type="submit">{submitting ? "발급 중" : "API Key 발급"}</button></div>
      </form>
    </Modal>
  );
}

function ConfirmActionDialog({ action, onClose, onConfirm }: { action: NonNullable<Action>; onClose: () => void; onConfirm: () => Promise<void> }) {
  const [pending, setPending] = useState(false);
  const reissue = action.type === "reissue";
  return (
    <Modal label={`${action.key.name} ${reissue ? "재발행" : "만료"}`} onClose={onClose}>
      <div className="confirm-dialog">
        <span className={reissue ? "dialog-icon" : "dialog-icon danger"}>{reissue ? <ArrowClockwise size={28} /> : <Trash size={28} />}</span>
        <h2>{reissue ? "API Key를 재발행할까요?" : "API Key를 만료할까요?"}</h2>
        <p><b>{action.key.name}</b>의 현재 키는 즉시 사용할 수 없게 됩니다.{reissue ? " 새 키 원문은 다음 화면에서 한 번만 표시됩니다." : " 이 작업은 되돌릴 수 없습니다."}</p>
        <code>{action.key.keyHint}</code>
        <div className="dialog-actions"><button onClick={onClose} type="button">취소</button><button className={reissue ? "primary-action" : "danger-action"} disabled={pending} onClick={() => { setPending(true); void onConfirm(); }} type="button">{pending ? "처리 중" : reissue ? "재발행" : "만료"}</button></div>
      </div>
    </Modal>
  );
}

function IssuedKeyDialog({ issued, onClose }: { issued: IssuedApiKey; onClose: () => void }) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    await navigator.clipboard.writeText(issued.apiKey);
    setCopied(true);
  }
  return (
    <Modal label="API Key 발급 완료" onClose={onClose}>
      <div className="issued-dialog">
        <span className="dialog-icon"><Check size={28} /></span>
        <p className="eyebrow">Issued once</p>
        <h2>지금 API Key를 복사하세요.</h2>
        <p>이 창을 닫으면 키 원문을 다시 확인할 수 없습니다. 분실하면 재발행해야 합니다.</p>
        <button className="issued-key" onClick={() => void copy()} type="button"><code>{issued.apiKey}</code>{copied ? <Check size={20} /> : <Copy size={20} />}</button>
        <p aria-live="polite" className="copy-status">{copied ? "클립보드에 복사했습니다." : "키를 눌러 복사할 수 있습니다."}</p>
        <button className="primary-action full" onClick={onClose} type="button">안전하게 보관했습니다</button>
      </div>
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

    const focusable = () => Array.from(modalRef.current?.querySelectorAll<HTMLElement>("button:not(:disabled), input:not(:disabled), textarea:not(:disabled)") ?? []);
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
