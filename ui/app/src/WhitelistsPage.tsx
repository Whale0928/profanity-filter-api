import { ArrowLeft, Check, Copy, ListChecks, Plus, Trash, X } from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type FormEvent } from "react";

import { Modal } from "./ApiKeysPage";
import {
  comparisonKey,
  createWhitelist,
  deleteWhitelist,
  formatDate,
  listWhitelists,
  MAX_GROUPS,
  MAX_GROUPS_PER_REQUEST,
  MAX_NAME_LENGTH,
  MAX_WORD_LENGTH,
  MAX_WORDS,
  requestExample,
  updateWhitelist,
  type WhitelistView,
} from "./whitelists";

const EXAMPLE_ID = "0b6f7c1e-5a3d-4c1e-9a53-2f8f4d6f1b20";
const PREVIEW_WORDS = 5;

/** 편집 중인 그룹입니다. id가 없으면 새로 만드는 그룹입니다. */
type Draft = { id: string | null; name: string; words: string[] };

export default function WhitelistsPage({ accessToken }: { accessToken: string }) {
  const [groups, setGroups] = useState<WhitelistView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [draft, setDraft] = useState<Draft | null>(null);
  const [copiedId, setCopiedId] = useState("");

  async function refresh() {
    setError("");
    try {
      setGroups(await listWhitelists(accessToken));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "허용 단어 그룹을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void refresh(); }, [accessToken]);

  async function copy(id: string) {
    await navigator.clipboard.writeText(id);
    setCopiedId(id);
    window.setTimeout(() => setCopiedId(current => (current === id ? "" : current)), 1600);
  }

  const totalWords = useMemo(() => groups.reduce((sum, group) => sum + group.wordCount, 0), [groups]);
  const full = groups.length >= MAX_GROUPS;

  if (draft) {
    return <GroupEditor
      accessToken={accessToken}
      copied={copiedId === draft.id}
      draft={draft}
      onBack={() => setDraft(null)}
      onCopy={copy}
      onDone={async () => { setDraft(null); await refresh(); }}
    />;
  }

  return (
    <section className="keys-page page-width">
      <header className="keys-heading">
        <div>
          <p className="eyebrow">Allowed Words</p>
          <h1>허용 단어 그룹</h1>
          <p>서비스에서 허용할 단어를 용도별로 묶어 둡니다. 필터 요청에 그룹 ID를 넣으면 적용됩니다.</p>
        </div>
        <button className="primary-action" disabled={full} onClick={() => setDraft({ id: null, name: "", words: [] })} type="button">
          <Plus size={18} /> 그룹 만들기
        </button>
      </header>

      <div aria-label="허용 단어 그룹 요약" className="keys-summary">
        <span>그룹 <b>{groups.length}</b> / {MAX_GROUPS}</span>
        <span>허용 단어 <b>{totalWords.toLocaleString("ko-KR")}</b></span>
        <span>요청당 최대 <b>{MAX_GROUPS_PER_REQUEST}</b>개 그룹</span>
      </div>

      {error ? <div className="keys-error" role="alert">{error}<button onClick={() => void refresh()} type="button">다시 시도</button></div> : null}
      {loading ? <div className="keys-state" role="status">허용 단어 그룹을 확인하고 있습니다.</div> : null}
      {!loading && !error && groups.length === 0 ? (
        <div className="keys-empty">
          <span><ListChecks size={28} /></span>
          <h2>아직 만든 허용 단어 그룹이 없습니다.</h2>
          <p>서비스 화면마다 허용할 단어가 다르면 그룹을 나눠 두세요.</p>
          <button onClick={() => setDraft({ id: null, name: "", words: [] })} type="button">첫 그룹 만들기</button>
        </div>
      ) : null}

      {groups.length > 0 ? (
        <div className="key-list">
          {groups.map(group => (
            <article className="key-row whitelist-row" key={group.id}>
              <div className="key-primary">
                <div><h2><button className="whitelist-name" onClick={() => setDraft({ id: group.id, name: group.name, words: group.words })} type="button">{group.name}</button></h2></div>
                <code>{group.id}</code>
                <p>단어 {group.wordCount}개 · {formatDate(group.updatedAt)} 수정</p>
              </div>
              <div className="whitelist-chips">
                {group.words.slice(0, PREVIEW_WORDS).map(word => <span className="whitelist-chip" key={word}>{word}</span>)}
                {group.wordCount > PREVIEW_WORDS ? <span className="whitelist-more">외 {group.wordCount - PREVIEW_WORDS}개</span> : null}
                {group.wordCount === 0 ? <span className="whitelist-more">등록한 단어 없음</span> : null}
              </div>
              <div className="key-actions">
                <button onClick={() => void copy(group.id)} type="button">{copiedId === group.id ? <Check size={17} /> : <Copy size={17} />} {copiedId === group.id ? "복사됨" : "ID 복사"}</button>
                <button onClick={() => setDraft({ id: group.id, name: group.name, words: group.words })} type="button">편집</button>
              </div>
            </article>
          ))}
        </div>
      ) : null}

      {!loading && !error ? (
        <div className="whitelist-usage">
          <div>
            <h2>요청에 그룹 ID 넣기</h2>
            <p>여러 그룹을 넣으면 허용 단어를 합쳐서 적용합니다.</p>
            <p>내 계정에 없는 그룹 ID가 있으면 요청이 실패합니다.</p>
          </div>
          <pre>{requestExample(groups.length ? groups.slice(0, 2).map(group => group.id) : [EXAMPLE_ID])}</pre>
        </div>
      ) : null}
    </section>
  );
}

function GroupEditor({ accessToken, copied, draft, onBack, onCopy, onDone }: {
  accessToken: string;
  copied: boolean;
  draft: Draft;
  onBack: () => void;
  onCopy: (id: string) => Promise<void>;
  onDone: () => Promise<void>;
}) {
  const [name, setName] = useState(draft.name);
  const [words, setWords] = useState<string[]>(draft.words);
  const [entry, setEntry] = useState("");
  const [formError, setFormError] = useState("");
  const [pending, setPending] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  /** 쉼표나 줄바꿈으로 여러 단어를 한 번에 넣을 수 있습니다. */
  function addWords(event: FormEvent) {
    event.preventDefault();
    setFormError("");
    const next = [...words];
    const keys = new Set(next.map(comparisonKey));
    for (const raw of entry.split(/[,\n]/)) {
      const word = raw.trim();
      if (!word) continue;
      const key = comparisonKey(word);
      if (!key) { setFormError(`한글이나 영문이 없는 단어는 등록할 수 없습니다: ${word}`); return; }
      if (word.length > MAX_WORD_LENGTH) { setFormError(`단어는 최대 ${MAX_WORD_LENGTH}자까지 가능합니다.`); return; }
      if (keys.has(key)) continue;
      keys.add(key);
      next.push(word);
    }
    if (next.length > MAX_WORDS) { setFormError(`허용 단어는 그룹당 최대 ${MAX_WORDS}개까지 가능합니다.`); return; }
    setWords(next);
    setEntry("");
  }

  async function save() {
    setFormError("");
    setPending(true);
    try {
      const input = { name: name.trim(), words };
      if (draft.id) await updateWhitelist(accessToken, draft.id, input);
      else await createWhitelist(accessToken, input);
      await onDone();
    } catch (requestError) {
      setFormError(requestError instanceof Error ? requestError.message : "저장하지 못했습니다.");
      setPending(false);
    }
  }

  async function remove() {
    if (!draft.id) return;
    try {
      await deleteWhitelist(accessToken, draft.id);
      await onDone();
    } catch (requestError) {
      setConfirmingDelete(false);
      setFormError(requestError instanceof Error ? requestError.message : "삭제하지 못했습니다.");
    }
  }

  return (
    <section className="keys-page whitelist-editor page-width">
      <header className="keys-heading">
        <div>
          <button className="whitelist-back" onClick={onBack} type="button"><ArrowLeft size={15} />허용 단어 그룹</button>
          <h1>{draft.id ? draft.name : "새 허용 단어 그룹"}</h1>
        </div>
        <div className="whitelist-editor-actions">
          {draft.id ? <button className="danger-text" disabled={pending} onClick={() => setConfirmingDelete(true)} type="button"><Trash size={17} />삭제</button> : null}
          <button className="primary-action" disabled={pending || !name.trim()} onClick={() => void save()} type="button">{pending ? "저장 중" : "저장"}</button>
        </div>
      </header>

      {formError ? <div className="keys-error" role="alert">{formError}</div> : null}

      <div className="whitelist-editor-grid">
        <div className="whitelist-fields">
          <label className="whitelist-field">
            <span>그룹 이름</span>
            <input maxLength={MAX_NAME_LENGTH} onChange={event => setName(event.target.value)} value={name} />
            <small>용도를 알아볼 수 있는 이름을 쓰세요.</small>
          </label>

          {draft.id ? (
            <div className="whitelist-field">
              <span>그룹 ID</span>
              <div className="whitelist-inline">
                <code>{draft.id}</code>
                <button onClick={() => void onCopy(draft.id as string)} type="button">{copied ? "복사됨" : "복사"}</button>
              </div>
              <small>이름을 바꿔도 ID는 그대로입니다.</small>
            </div>
          ) : null}

          <div className="whitelist-field">
            <div className="whitelist-field-head"><label htmlFor="whitelist-word">허용 단어</label><span>{words.length} / {MAX_WORDS}</span></div>
            <form className="whitelist-inline" onSubmit={addWords}>
              <input id="whitelist-word" onChange={event => setEntry(event.target.value)} placeholder="단어 입력" value={entry} />
              <button className="whitelist-add" disabled={!entry.trim()} type="submit">추가</button>
            </form>
            <div className="whitelist-box">
              {words.map(word => (
                <span className="whitelist-chip is-removable" key={word}>{word}
                  <button aria-label={`${word} 삭제`} onClick={() => setWords(words.filter(item => item !== word))} type="button"><X size={12} /></button>
                </span>
              ))}
              {words.length === 0 ? <p>아직 등록한 단어가 없습니다.</p> : null}
            </div>
            <small>검출된 단어가 여기 있으면 결과와 마스킹에서 빠집니다.</small>
          </div>
        </div>

        <aside className="whitelist-guide">
          <h2>요청 예시</h2>
          <pre>{requestExample([draft.id ?? EXAMPLE_ID])}</pre>
          <p>저장하면 1분 안에 반영됩니다.</p>
          <p>그룹 ID를 넣지 않은 요청은 기존과 같습니다.</p>
          <p>그룹을 삭제하면 이 ID를 쓰는 요청이 실패합니다.</p>
        </aside>
      </div>

      {confirmingDelete ? (
        <Modal label={`${draft.name} 삭제`} onClose={() => setConfirmingDelete(false)}>
          <div className="confirm-dialog">
            <span className="dialog-icon danger"><Trash size={28} /></span>
            <h2>허용 단어 그룹을 삭제할까요?</h2>
            <p><b>{draft.name}</b>을 삭제하면 이 ID를 쓰는 필터 요청이 실패합니다. 서비스 코드에서 ID를 먼저 빼 주세요.</p>
            <code>{draft.id}</code>
            <div className="dialog-actions"><button onClick={() => setConfirmingDelete(false)} type="button">취소</button><button className="danger-action" onClick={() => void remove()} type="button">삭제</button></div>
          </div>
        </Modal>
      ) : null}
    </section>
  );
}
