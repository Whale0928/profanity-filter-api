import { useEffect, useState } from "react";
import { ArrowLeft, ArrowUpRight, Check, MagnifyingGlass, PencilSimple, Plus, ShieldCheck, Trash, X, CaretLeft, CaretRight, NotePencil, BookOpen, ChatCircleText, Users, Key } from "@phosphor-icons/react";
import "./AdminPage.css";
import AdminMockPanels from "./AdminMockPanels";
import AdminPanels from "./AdminPanels";
const menus = [{ id: "news", label: "소식 관리", icon: NotePencil }, { id: "dictionary", label: "필터 사전", icon: BookOpen }, { id: "inquiries", label: "문의", icon: ChatCircleText }, { id: "users", label: "사용자 관리", icon: Users }, { id: "keys", label: "API Key 관리", icon: Key }] as const;
type Menu = typeof menus[number]["id"];
import MarkdownDocument from "./docs/MarkdownDocument";
import { createNews, deleteNews, listAdminNews, updateNews, NEWS_CATEGORIES, type NewsCategory, type NewsPost, type NewsStatus } from "./news";
import { removeMockPost, saveMockPost, useMockPosts, type MockPost } from "./newsMock";
import { NewsBadge, newsDate } from "./NewsPage";

type DraftForm = { category: NewsCategory; content: string; title: string };
type PublishState = "draft" | "published";

export default function AdminPage({
  accessToken,
  allowed,
  currentUserId,
  onPublic,
  preview,
}: {
  accessToken: string | null;
  allowed: boolean;
  currentUserId: string | null;
  onPublic: () => void;
  preview: boolean;
}) {
  const posts = useMockPosts();
  const [menu, setMenu] = useState<Menu>("news");
  const [collapsed, setCollapsed] = useState(() => window.localStorage.getItem("pf-admin-sidebar-collapsed") === "true");
  useEffect(() => { window.localStorage.setItem("pf-admin-sidebar-collapsed", String(collapsed)); }, [collapsed]);
  const [filter, setFilter] = useState<"all" | MockPost["state"]>("all");
  const [query, setQuery] = useState("");
  const [draft, setDraft] = useState<DraftForm | null>(null);
  const [editingId, setEditingId] = useState<number | string>();
  const [editingWasPublished, setEditingWasPublished] = useState(false);
  const [view, setView] = useState<"write" | "preview" | "split">("split");
  const [message, setMessage] = useState("");
  const [deleteId, setDeleteId] = useState<number | string | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [discard, setDiscard] = useState(false);
  const [pendingMenu, setPendingMenu] = useState<Menu | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");

  const [newsItems, setNewsItems] = useState<NewsPost[]>([]);
  const [newsPage, setNewsPage] = useState(0);
  const [newsHasNext, setNewsHasNext] = useState(false);
  const [newsLoading, setNewsLoading] = useState(!preview);
  const [newsError, setNewsError] = useState("");
  const [newsRetry, setNewsRetry] = useState(0);
  const [debouncedQuery, setDebouncedQuery] = useState("");

  useEffect(() => {
    if (!draft) return;
    const warn = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [draft]);

  useEffect(() => {
    const timer = window.setTimeout(() => { setDebouncedQuery(query); setNewsPage(0); }, 300);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    if (preview || !accessToken || menu !== "news" || draft) return;
    const status: NewsStatus | "" = filter === "all" ? "" : filter === "published" ? "PUBLISHED" : "DRAFT";
    const controller = new AbortController();
    setNewsLoading(true); setNewsError("");
    listAdminNews(accessToken, status, "", debouncedQuery, newsPage, controller.signal)
      .then(result => { setNewsItems(result.items); setNewsHasNext(result.hasNext); })
      .catch(() => { if (!controller.signal.aborted) setNewsError("소식 목록을 불러오지 못했습니다."); })
      .finally(() => { if (!controller.signal.aborted) setNewsLoading(false); });
    return () => controller.abort();
  }, [accessToken, preview, menu, draft, filter, debouncedQuery, newsPage, newsRetry]);

  if (!allowed) return <section className="updates-page page-width"><div className="admin-denied"><ShieldCheck size={26} /><h1>관리자 전용 페이지입니다.</h1><p>관리 권한이 있는 계정으로 로그인해 주세요.</p><button className="compact-button" type="button" onClick={onPublic}>소식으로 돌아가기</button></div></section>;

  const count = (state: MockPost["state"]) => posts.filter(post => post.state === state).length;
  const visible = preview
    ? posts.filter(post => (filter === "all" || post.state === filter) && post.title.toLowerCase().includes(query.toLowerCase()))
    : [];

  function edit(post?: MockPost | NewsPost) {
    setDraft(post ? { category: post.category, content: post.content, title: post.title } : { category: "NOTICE", content: "", title: "" });
    setEditingId(post?.id);
    setEditingWasPublished(post ? (preview ? (post as MockPost).state === "published" : (post as NewsPost).status === "PUBLISHED") : false);
    setMessage(""); setSaveError("");
  }

  async function save(state: PublishState) {
    if (!draft?.title.trim() || !draft.content.trim()) return;
    setSaving(true); setSaveError("");
    try {
      if (preview) {
        saveMockPost(draft, state, editingId === undefined ? undefined : String(editingId));
        setMessage(state === "published" ? "목업에서 게시했습니다. 소식 페이지에서 확인할 수 있습니다." : "목업에 임시 저장했습니다.");
      } else if (accessToken) {
        const payload = { category: draft.category, content: draft.content, status: (state === "published" ? "PUBLISHED" : "DRAFT") as NewsStatus, title: draft.title };
        if (editingId) await updateNews(accessToken, editingId, payload); else await createNews(accessToken, payload);
        setNewsRetry(n => n + 1);
        setMessage(state === "published"
          ? (editingId ? "변경사항을 게시했습니다." : "게시했습니다. 소식 페이지에서 확인할 수 있습니다.")
          : (editingWasPublished ? "게시된 소식을 비공개로 전환하고 임시 저장했습니다." : "임시 저장했습니다."));
      }
      setDraft(null);
    } catch (requestError) {
      setSaveError(requestError instanceof Error ? requestError.message : "저장하지 못했습니다.");
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!deleteId) return;
    setDeleteError("");
    if (preview) {
      removeMockPost(String(deleteId));
      setMessage("목업에서 삭제했습니다.");
      setDeleteId(null);
      return;
    }
    if (!accessToken) return;
    try {
      await deleteNews(accessToken, deleteId);
      setNewsRetry(n => n + 1);
      setMessage("소식을 삭제했습니다.");
      setDeleteId(null);
    } catch (requestError) {
      setDeleteError(requestError instanceof Error ? requestError.message : "소식을 삭제하지 못했습니다.");
    }
  }

  return <section className={`console-shell page-width ${collapsed ? "console-collapsed" : ""}`}>
    <aside className="console-sidebar">
      <div className="console-brand"><ShieldCheck size={20} /><div className="console-label"><strong>관리자</strong></div></div>
      <nav aria-label="관리자 메뉴" className="console-navigation">
        {menus.map(item => <button key={item.id} aria-label={item.label} title={item.label} aria-current={menu === item.id ? "page" : undefined} type="button" onClick={() => { if (draft) { setPendingMenu(item.id); setDiscard(true); } else { setMenu(item.id); setMessage(""); } }}><item.icon size={17} /><span className="console-label">{item.label}</span></button>)}
      </nav>
      <button className="console-collapse" aria-label={collapsed ? "사이드바 펼치기" : "사이드바 접기"} aria-expanded={!collapsed} type="button" onClick={() => setCollapsed(value => !value)}>{collapsed ? <CaretRight size={18} /> : <CaretLeft size={18} />}<span className="console-label">사이드바 접기</span></button>
    </aside>
    <div className="console-workspace">
      <header className="console-topbar"><div><h1>{draft ? (editingId ? "소식 수정" : "새 소식 작성") : menus.find(item => item.id === menu)?.label}</h1></div>{menu === "news" && !draft && <button className="compact-button filled" type="button" onClick={() => edit()}><Plus size={14} />새 소식</button>}</header>
      <div className="admin-main">
      {menu !== "news" ? (
        preview
          ? <AdminMockPanels key={menu} menu={menu} />
          : accessToken ? <AdminPanels accessToken={accessToken} currentUserId={currentUserId} key={menu} menu={menu} /> : null
      ) : draft ? <>
        <div className="admin-editor-top"><button className="updates-back" type="button" onClick={() => setDiscard(true)}><ArrowLeft size={15} />소식 관리</button><div><button className="compact-button" type="button" disabled={!draft.title.trim() || !draft.content.trim() || saving} onClick={() => void save("draft")}>임시 저장</button><button className="compact-button filled" type="button" disabled={!draft.title.trim() || !draft.content.trim() || saving} onClick={() => void save("published")}>{editingId ? "변경사항 게시" : "게시하기"}<ArrowUpRight size={14} /></button></div></div>
        {!preview && editingWasPublished ? <p className="mock-hint">이미 게시된 소식입니다. 임시 저장하면 즉시 비공개로 전환되어 소식 페이지에서 보이지 않게 됩니다.</p> : null}
        {saveError ? <p className="form-error" role="alert">{saveError}</p> : null}
        <div className="console-editor-caption"><NotePencil size={16} /><p>Markdown으로 작성하고 게시 전 미리보기를 확인하세요.</p></div>
        <div className="admin-fields"><label>유형<select value={draft.category} onChange={event => setDraft({ ...draft, category: event.target.value as NewsCategory })}>{Object.entries(NEWS_CATEGORIES).map(([key, label]) => <option value={key} key={key}>{label}</option>)}</select></label><label className="admin-title-field">제목<input maxLength={160} value={draft.title} onChange={event => setDraft({ ...draft, title: event.target.value })} placeholder="소식 제목을 입력하세요" /></label></div>
        <div className="admin-compose-toolbar"><span>본문 <small>Markdown</small></span><div role="group" aria-label="편집 화면"><button type="button" aria-pressed={view === "write"} onClick={() => setView("write")}>작성</button><button type="button" aria-pressed={view === "split"} onClick={() => setView("split")}>나란히</button><button type="button" aria-pressed={view === "preview"} onClick={() => setView("preview")}>미리보기</button></div></div>
        <div className={`admin-compose view-${view}`}>{view !== "preview" ? <label className="admin-source"><span>MARKDOWN</span><textarea aria-label="Markdown 본문" maxLength={50000} value={draft.content} onChange={event => setDraft({ ...draft, content: event.target.value })} placeholder={"## 주요 변경 사항\n\n- 새로운 내용을 작성하세요.\n\n## 적용 일정\n\n서비스 이용에 필요한 내용을 안내하세요."} /></label> : null}{view !== "write" ? <div className="admin-rendered"><span>미리보기</span>{draft.content ? <MarkdownDocument content={draft.content} /> : <p className="admin-preview-empty">작성한 내용이 여기에 표시됩니다.</p>}</div> : null}</div>
        <div className="admin-editor-foot"><span>제목 · 목록 · 코드 · 표 · 인용 · 링크 지원</span><span>{draft.content.length.toLocaleString()} / 50,000자</span></div>
      </> : <>
        <div className="console-panel"><header className="admin-header"><p>공지, 변경 내역과 운영 소식을 관리합니다.</p></header>
        {message ? <div className="admin-feedback" role="status"><Check size={15} />{message}<button type="button" aria-label="알림 닫기" onClick={() => setMessage("")}><X size={15} /></button></div> : null}
        <div className="admin-list-toolbar"><div className="admin-status-tabs" role="group" aria-label="게시 상태">{([ ["all", "전체", preview ? posts.length : undefined], ["published", "게시됨", preview ? count("published") : undefined], ["draft", "임시 저장", preview ? count("draft") : undefined] ] as const).map(([key, label, total]) => <button type="button" aria-pressed={filter === key} key={key} onClick={() => { setFilter(key); setNewsPage(0); }}>{label}{total !== undefined ? <span>{total}</span> : null}</button>)}</div><label className="updates-search"><MagnifyingGlass size={15} /><input aria-label="관리할 소식 검색" placeholder="제목 검색" value={query} onChange={event => setQuery(event.target.value)} /></label></div>
        {!preview && newsError ? <div className="keys-error" role="alert">{newsError}<button onClick={() => setNewsRetry(n => n + 1)} type="button">다시 시도</button></div> : null}
        {!preview && newsLoading ? <p className="updates-empty" role="status">소식 목록을 불러오고 있습니다.</p> : (
          <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>제목</th><th>상태</th><th>수정일</th><th><span className="sr-only">작업</span></th></tr></thead><tbody>
            {preview
              ? visible.map(post => <tr key={post.id}><td><div className="admin-title-cell"><button className="admin-post-title" onClick={() => edit(post)} type="button">{post.title}</button><NewsBadge category={post.category} /></div><span className="console-mobile-state">{post.state === "published" ? "게시됨" : "임시 저장"}</span></td><td><span className={`admin-status state-${post.state}`}><i />{post.state === "published" ? "게시됨" : "임시 저장"}</span></td><td><time dateTime={post.updatedAt}>{newsDate(post.updatedAt)}</time></td><td><div className="admin-row-actions"><button type="button" aria-label={`${post.title} 수정`} onClick={() => edit(post)}><PencilSimple size={16} /></button><button type="button" aria-label={`${post.title} 삭제`} onClick={() => setDeleteId(post.id)}><Trash size={16} /></button></div></td></tr>)
              : newsItems.map(post => <tr key={post.id}><td><div className="admin-title-cell"><button className="admin-post-title" onClick={() => edit(post)} type="button">{post.title}</button><NewsBadge category={post.category} /></div><span className="console-mobile-state">{post.status === "PUBLISHED" ? "게시됨" : "임시 저장"}</span></td><td><span className={`admin-status state-${post.status === "PUBLISHED" ? "published" : "draft"}`}><i />{post.status === "PUBLISHED" ? "게시됨" : "임시 저장"}</span></td><td><time dateTime={post.updatedAt}>{newsDate(post.updatedAt)}</time></td><td><div className="admin-row-actions"><button type="button" aria-label={`${post.title} 수정`} onClick={() => edit(post)}><PencilSimple size={16} /></button><button type="button" aria-label={`${post.title} 삭제`} onClick={() => setDeleteId(post.id)}><Trash size={16} /></button></div></td></tr>)}
          </tbody></table>{(preview ? !visible.length : !newsItems.length) ? <p className="updates-empty">조건에 맞는 소식이 없습니다.</p> : null}</div>
        )}
        <p className="admin-table-note">{preview ? visible.length : newsItems.length}개의 소식 · 게시된 글만 소식 페이지에 표시됩니다.</p>
        {!preview && !newsLoading && !newsError && (newsPage > 0 || newsHasNext) ? <nav aria-label="소식 페이지" className="updates-pagination"><button disabled={!newsPage} onClick={() => setNewsPage(n => n - 1)} type="button">이전</button><span>{newsPage + 1}</span><button disabled={!newsHasNext} onClick={() => setNewsPage(n => n + 1)} type="button">다음</button></nav> : null}
        </div>
      </>}
      {deleteId || discard ? <dialog className="admin-dialog" aria-labelledby="admin-dialog-title" ref={node => { if (node && !node.open) node.showModal(); }} onCancel={() => { setDeleteId(null); setDeleteError(""); setDiscard(false); setPendingMenu(null); }}>
        <h2 id="admin-dialog-title">{deleteId ? "이 소식을 삭제할까요?" : "작성을 그만둘까요?"}</h2>
        <p>{deleteId ? (preview ? "목업 목록에서 삭제됩니다. 새로고침하면 예시 데이터가 복원됩니다." : "삭제한 소식은 되돌릴 수 없습니다.") : "저장하지 않은 내용은 사라집니다."}</p>
        {deleteId && deleteError ? <p className="form-error" role="alert">{deleteError}</p> : null}
        <div>
          <button autoFocus type="button" className="compact-button" onClick={() => { setDeleteId(null); setDeleteError(""); setDiscard(false); setPendingMenu(null); }}>취소</button>
          <button className="compact-button filled" type="button" onClick={() => { if (deleteId) { void confirmDelete(); } if (discard) { setDraft(null); if (pendingMenu) setMenu(pendingMenu); setPendingMenu(null); setDiscard(false); } }}>{deleteId ? "삭제" : "그만두기"}</button>
        </div>
      </dialog> : null}
      </div>

    </div>
  </section>;
}
