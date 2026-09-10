import { useEffect, useState } from "react";
import { ArrowLeft, ArrowUpRight, CaretRight, MagnifyingGlass } from "@phosphor-icons/react";
import MarkdownDocument from "./docs/MarkdownDocument";
import { NEWS_CATEGORIES, getNews, listNews, type NewsCategory, type NewsPost, type NewsSummary } from "./news";
import { useMockPosts } from "./newsMock";

function selectedId() { return new URLSearchParams(window.location.search).get("post"); }
export function newsDate(value: string) { return new Date(value).toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit", timeZone: "Asia/Seoul" }).replace(/\. /g, ".").replace(/\.$/, ""); }
export function NewsBadge({ category }: { category: NewsCategory }) { return <span className={`news-badge category-${category.toLowerCase()}`}>{NEWS_CATEGORIES[category]}</span>; }

export default function NewsPage({ onManage, preview = false }: { onManage?: () => void; preview?: boolean }) {
  const mockPosts = useMockPosts();
  const [id, setId] = useState(selectedId);
  const [category, setCategory] = useState<NewsCategory | "">("");
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<NewsSummary[]>([]);
  const [post, setPost] = useState<NewsPost | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(!preview);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => { const pop = () => setId(selectedId()); window.addEventListener("popstate", pop); return () => window.removeEventListener("popstate", pop); }, []);
  useEffect(() => {
    const timer = window.setTimeout(() => { setDebouncedSearch(search); setPage(0); }, 300);
    return () => window.clearTimeout(timer);
  }, [search]);
  useEffect(() => {
    if (preview) { setLoading(false); return; }
    const controller = new AbortController(); setLoading(true); setError(""); setPost(null);
    const request = id ? getNews(id, controller.signal).then(setPost) : listNews(category, debouncedSearch, page, controller.signal).then(result => { setItems(result.items); setHasNext(result.hasNext); });
    request.catch(() => { if (!controller.signal.aborted) setError("소식을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, category, debouncedSearch, page, retry, preview]);
  const published = mockPosts.filter(item => item.state === "published");
  const current = preview ? published.find(item => item.id === id) : post;
  const visible = preview ? published.filter(item => (!category || item.category === category) && item.title.toLowerCase().includes(search.toLowerCase())) : items;
  function open(next: number | string | null) { const params = new URLSearchParams(window.location.search); if (next !== null) params.set("post", String(next)); else params.delete("post"); window.history.pushState({}, "", `/news${params.size ? `?${params}` : ""}`); setId(next === null ? null : String(next)); window.scrollTo({ top: 0 }); }
  const relatedSource = preview ? published : items;
  const adjacent = current ? relatedSource.filter(item => item.id !== current.id).slice(0, 2) : [];
  return <section className="updates-page page-width">
    {id ? <>
      <div className="updates-breadcrumb"><button type="button" onClick={() => open(null)}><ArrowLeft size={14} />소식 목록</button><span>/</span><span>{current ? NEWS_CATEGORIES[current.category] : "상세"}</span>{onManage ? <button className="updates-manage" type="button" onClick={onManage}>소식 관리<ArrowUpRight size={14} /></button> : null}</div>
      {loading ? <p role="status" className="updates-empty">소식을 불러오고 있습니다.</p> : current ? <div className="updates-detail-layout"><article className="updates-article">
        <header><NewsBadge category={current.category} /><h1>{current.title}</h1><div className="updates-byline"><span className="updates-author-mark">말</span><span>말조심하세욧 팀</span><span>·</span><time dateTime={current.createdAt}>{newsDate(current.createdAt)}</time>{current.updatedAt !== current.createdAt ? <span>수정됨</span> : null}</div></header>
        <MarkdownDocument content={current.content} />
        <div className="updates-article-end"><span>말조심하세욧의 새로운 소식을 확인해 주세요.</span><button type="button" onClick={() => open(null)}>목록으로<ArrowLeft size={14} /></button></div>
      </article><aside className="updates-related"><p>다른 소식</p>{adjacent.map(item => <button type="button" key={item.id} onClick={() => open(item.id)}><NewsBadge category={item.category} /><strong>{item.title}</strong><time>{newsDate(item.createdAt)}</time></button>)}</aside></div> : <div className="updates-empty"><p role="alert">{error || "게시글을 찾을 수 없습니다."}</p><button type="button" onClick={() => { if (error) setRetry(n => n + 1); else open(null); }}>{error ? "다시 시도" : "소식 목록"}</button></div>}
    </> : <>
      <header className="updates-header"><div><h1>소식</h1><p>새로운 기능, 서비스 운영과 업데이트 안내</p></div>{onManage ? <button type="button" className="updates-manage" onClick={onManage}>소식 관리<ArrowUpRight size={14} /></button> : null}</header>
      <div className="updates-toolbar"><div className="updates-tabs" role="group" aria-label="소식 유형"><button type="button" aria-pressed={!category} onClick={() => { setCategory(""); setPage(0); }}>전체</button>{Object.entries(NEWS_CATEGORIES).map(([key, label]) => <button key={key} type="button" aria-pressed={category === key} onClick={() => { setCategory(key as NewsCategory); setPage(0); }}>{label}</button>)}</div><label className="updates-search"><MagnifyingGlass size={16} /><input value={search} onChange={event => setSearch(event.target.value)} placeholder="제목 검색" aria-label="소식 제목 검색" /></label></div>
      {loading ? <p className="updates-empty" role="status">소식을 불러오고 있습니다.</p> : error ? <div className="updates-empty"><p role="alert">{error}</p><button type="button" onClick={() => setRetry(n => n + 1)}>다시 시도</button></div> : <>
        <div className="updates-list-heading"><span>{preview ? `${visible.length}개의 소식` : "최신 소식"}</span><span>최신순</span></div>
        <ul className="updates-list">{visible.map(item => <li key={item.id}><a href={`/news?post=${item.id}`} onClick={event => { if (event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey) { event.preventDefault(); open(item.id); } }}><NewsBadge category={item.category} /><div><h2>{item.title}</h2>{"summary" in item && typeof item.summary === "string" ? <p>{item.summary}</p> : null}</div><time dateTime={item.createdAt}>{newsDate(item.createdAt)}</time><CaretRight size={15} /></a></li>)}</ul>
        {!visible.length ? <div className="updates-empty"><p>{search ? "검색 결과가 없습니다." : "등록된 소식이 없습니다."}</p>{search ? <button type="button" onClick={() => setSearch("")}>검색 초기화</button> : null}</div> : null}
        {!preview && (page > 0 || hasNext) ? <nav className="updates-pagination" aria-label="소식 페이지"><button type="button" disabled={!page} onClick={() => setPage(n => n - 1)}>이전</button><span>{page + 1}</span><button type="button" disabled={!hasNext} onClick={() => setPage(n => n + 1)}>다음</button></nav> : null}
      </>}
    </>}
  </section>;
}
