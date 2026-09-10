import { CaretLeft, CaretRight } from "@phosphor-icons/react";
import { lazy, Suspense, useEffect, useMemo, useRef, useState, type MouseEvent, type ReactNode } from "react";

import type { Theme } from "../types";
import MarkdownDocument from "./MarkdownDocument";
import { OPENAPI_URL, OVERVIEW_URL, useOpenApiDocument, useOverviewDocument } from "./documents";
import {
  buildApiNavigation,
  buildMarkdownNavigation,
  createOperationAnchor,
  createTagAnchor,
} from "./navigation";

const ScalarReference = lazy(() => import("./ScalarReference"));

function getDecodedHash() {
  const rawHash = window.location.hash.replace(/^#/, "");
  if (!rawHash) return "";
  try {
    const decoded = decodeURIComponent(rawHash);
    return decoded.startsWith("tag/") ? `docs/${decoded}` : decoded;
  } catch {
    return rawHash.startsWith("tag/") ? `docs/${rawHash}` : rawHash;
  }
}

export default function DocsPage({ theme }: { theme: Theme }) {
  const [activeHash, setActiveHash] = useState(getDecodedHash);
  const [menuOpen, setMenuOpen] = useState(false);
  const [openGroups, setOpenGroups] = useState<Set<string>>(() => new Set());
  const [pendingAnchor, setPendingAnchor] = useState<string | null>(() => getDecodedHash() || null);
  const pendingAnchorRef = useRef(pendingAnchor);
  const sidebarRef = useRef<HTMLElement>(null);
  const overview = useOverviewDocument();
  const openApi = useOpenApiDocument();
  const markdownNavigation = useMemo(() => buildMarkdownNavigation(overview.data ?? ""), [overview.data]);
  const apiNavigation = useMemo(() => buildApiNavigation(openApi.data ?? {}), [openApi.data]);
  const isReference = activeHash.startsWith("docs/tag/");
  const activeGroup = apiNavigation.find((group) => activeHash.startsWith(`${createTagAnchor(group)}/`));
  const activeOperation = activeGroup?.operations.find((operation) => createOperationAnchor(operation) === activeHash);

  useEffect(() => {
    void import("./ScalarReference");
  }, []);

  useEffect(() => {
    const syncHash = () => {
      const next = getDecodedHash();
      setActiveHash(next);
      // 클릭이 아니라 주소로 들어온 해시도 정착할 때까지 스크롤 추적을 멈춰야
      // 추적이 주소를 직전 위치로 되돌려 화면과 주소가 어긋나는 일이 없다.
      if (next) setPendingAnchor(next);
    };
    window.addEventListener("hashchange", syncHash);
    window.addEventListener("popstate", syncHash);
    return () => {
      window.removeEventListener("hashchange", syncHash);
      window.removeEventListener("popstate", syncHash);
    };
  }, []);

  useEffect(() => {
    pendingAnchorRef.current = pendingAnchor;
  }, [pendingAnchor]);

  useEffect(() => {
    if (!activeGroup) return;
    setOpenGroups((current) => {
      if (current.has(activeGroup.slug)) return current;
      return new Set([...current, activeGroup.slug]);
    });
  }, [activeHash, apiNavigation]);

  useEffect(() => {
    const sidebar = sidebarRef.current;
    if (!sidebar) return;
    const activeItem =
      sidebar.querySelector<HTMLElement>('.docs-api-operations a[aria-current="page"]') ??
      sidebar.querySelector<HTMLElement>('.docs-overview-navigation a[aria-current="page"]') ??
      sidebar.querySelector<HTMLElement>('.docs-api-group > button.active');
    if (!activeItem) return;

    const sidebarRect = sidebar.getBoundingClientRect();
    const activeRect = activeItem.getBoundingClientRect();
    const safeInset = 28;
    const isVisible = activeRect.top >= sidebarRect.top + safeInset && activeRect.bottom <= sidebarRect.bottom - safeInset;
    if (isVisible) return;

    const targetTop = sidebar.scrollTop + activeRect.top - sidebarRect.top - sidebar.clientHeight / 2 + activeRect.height / 2;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    sidebar.scrollTo({ top: Math.max(0, targetTop), behavior: reduceMotion ? "auto" : "smooth" });
  }, [activeHash, openGroups]);

  useEffect(() => {
    if (!pendingAnchor) return;
    let releaseTimer = 0;
    let intervalTimer = 0;
    let finished = false;
    let attempts = 0;
    let stablePasses = 0;

    const finish = () => {
      if (finished) return;
      finished = true;
      observer.disconnect();
      window.clearInterval(intervalTimer);
      releaseTimer = window.setTimeout(() => setPendingAnchor(null), 450);
    };

    const alignTarget = () => {
      const target = window.document.getElementById(pendingAnchor);
      if (!target) return;
      const targetTop = target.getBoundingClientRect().top;
      if (targetTop < 0 || targetTop > 32) {
        target.scrollIntoView({ block: "start", behavior: "auto" });
      }
    };

    const verifyTarget = () => {
      attempts += 1;
      const target = window.document.getElementById(pendingAnchor);
      if (!target) {
        if (attempts >= 60) finish();
        return;
      }
      const targetTop = target.getBoundingClientRect().top;
      const aligned = targetTop >= 0 && targetTop <= 32;
      if (aligned) stablePasses += 1;
      else {
        stablePasses = 0;
        alignTarget();
      }
      if (stablePasses >= 8 || attempts >= 60) finish();
    };

    const observer = new MutationObserver(alignTarget);
    observer.observe(window.document.body, { childList: true, subtree: true });
    intervalTimer = window.setInterval(verifyTarget, 100);
    alignTarget();

    return () => {
      observer.disconnect();
      window.clearInterval(intervalTimer);
      window.clearTimeout(releaseTimer);
    };
  }, [pendingAnchor, isReference, openApi.data, overview.data]);

  useEffect(() => {
    if (!isReference || !openApi.data || pendingAnchor) return;

    const operationAnchors = apiNavigation.flatMap((group) => group.operations.map(createOperationAnchor));
    if (operationAnchors.length === 0) return;

    // 화면 위 180px 지점을 지나는 항목이 현재 항목이다. 관측을 브라우저에 맡기므로,
    // 스크롤 프레임마다 getBoundingClientRect 로 레이아웃을 강제로 읽던 이전 방식과 달리 드래그가 끊기지 않는다.
    const crossing = new Set<string>();
    const observed = new Set<string>();
    let observer: IntersectionObserver | null = null;
    let attachTimer = 0;

    const commit = () => {
      if (pendingAnchorRef.current) return;
      let nextHash = "";
      for (const anchor of operationAnchors) if (crossing.has(anchor)) nextHash = anchor;
      if (!nextHash) return;
      setActiveHash((current) => {
        if (current === nextHash) return current;
        window.history.replaceState({}, "", `/docs#${encodeURIComponent(nextHash)}`);
        return nextHash;
      });
    };

    const attach = () => {
      for (const anchor of operationAnchors) {
        if (observed.has(anchor)) continue;
        const element = window.document.getElementById(anchor);
        if (!element) continue;
        observed.add(anchor);
        observer?.observe(element);
      }
    };

    const build = () => {
      observer?.disconnect();
      crossing.clear();
      observed.clear();
      const bottomMargin = Math.min(-1, 181 - window.innerHeight);
      observer = new IntersectionObserver(
        (entries) => {
          for (const entry of entries) {
            if (entry.isIntersecting) crossing.add(entry.target.id);
            else crossing.delete(entry.target.id);
          }
          commit();
        },
        { rootMargin: `-180px 0px ${bottomMargin}px 0px` },
      );
      attach();
    };

    build();

    // Scalar 가 본문을 나중에 그리므로 새로 생긴 앵커만 관측 대상에 더한다.
    const mutation = new MutationObserver(() => {
      if (attachTimer) return;
      attachTimer = window.setTimeout(() => {
        attachTimer = 0;
        attach();
      }, 120);
    });
    const reference = window.document.querySelector(".api-docs-reference");
    if (reference) mutation.observe(reference, { childList: true, subtree: true });

    const onResize = () => build();
    window.addEventListener("resize", onResize);

    return () => {
      observer?.disconnect();
      mutation.disconnect();
      window.removeEventListener("resize", onResize);
      window.clearTimeout(attachTimer);
    };
  }, [apiNavigation, isReference, openApi.data, pendingAnchor]);

  const toggleGroup = (slug: string) => {
    setOpenGroups((current) => {
      const next = new Set(current);
      if (next.has(slug)) next.delete(slug);
      else next.add(slug);
      return next;
    });
  };

  const navigateToAnchor = (event: MouseEvent<HTMLAnchorElement>, anchor: string) => {
    if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    setMenuOpen(false);
    pendingAnchorRef.current = anchor;
    setPendingAnchor(anchor);
    setActiveHash(anchor);
    window.history.pushState({}, "", `/docs#${encodeURIComponent(anchor)}`);
  };

  const activeMarkdownAnchor = activeHash || markdownNavigation[0]?.anchor;
  const returnToApiMenu = () => { setMenuOpen(true); sidebarRef.current?.scrollIntoView({ block: "start", behavior: "smooth" }); };

  return (
    <section className="docs-page" data-section={isReference ? "reference" : "overview"}>
      <aside aria-label="API 문서 메뉴" ref={sidebarRef}>
        <div className="docs-menu-heading"><p>API 문서</p><button className="docs-menu-toggle" aria-expanded={menuOpen} aria-controls="docs-navigation" onClick={() => setMenuOpen((open) => !open)} type="button">{menuOpen ? "목차 닫기" : "목차 열기"}<CaretRight size={16} /></button></div>
        <nav id="docs-navigation" className={`docs-sidebar-nav${menuOpen ? " is-open" : ""}`}>
          <div className="docs-overview-navigation">
            {markdownNavigation.map((item) => (
              <a
                aria-current={activeMarkdownAnchor === item.anchor ? "page" : undefined}
                href={`/docs#${encodeURIComponent(item.anchor)}`}
                key={item.anchor}
                onClick={(event) => navigateToAnchor(event, item.anchor)}
              >
                {item.title}
              </a>
            ))}
          </div>

          <div aria-hidden="true" className="docs-sidebar-divider" />

          <div className="docs-api-navigation">
            {apiNavigation.map((group) => {
              const expanded = openGroups.has(group.slug);
              const groupActive = activeHash.startsWith(`${createTagAnchor(group)}/`);
              return (
                <div className="docs-api-group" key={group.slug}>
                  <button
                    aria-controls={`docs-api-group-${group.slug}`}
                    aria-expanded={expanded}
                    className={groupActive ? "active" : undefined}
                    onClick={() => toggleGroup(group.slug)}
                    type="button"
                  >
                    <span>{group.name}</span>
                    <CaretRight aria-hidden="true" size={14} weight="bold" />
                  </button>
                  {expanded ? (
                    <div className="docs-api-operations" id={`docs-api-group-${group.slug}`}>
                      {group.operations.map((operation) => {
                        const anchor = createOperationAnchor(operation);
                        return (
                          <a
                            aria-current={activeHash === anchor ? "page" : undefined}
                            href={`/docs#${encodeURIComponent(anchor)}`}
                            key={`${operation.method}-${operation.path}`}
                            onClick={(event) => navigateToAnchor(event, anchor)}
                          >
                            {operation.summary}
                          </a>
                        );
                      })}
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        </nav>
      </aside>

      <article className={isReference ? "docs-reference-article" : undefined}>
        {isReference ? (
          <div className="docs-mobile-context">
            <button onClick={returnToApiMenu} type="button"><CaretLeft aria-hidden="true" size={15} />API 메뉴</button>
            <span aria-label="현재 API 위치">{activeGroup?.name ?? "API"}{activeOperation ? ` / ${activeOperation.summary}` : ""}</span>
          </div>
        ) : null}
        <div className="docs-source-bar">
          <span>{isReference ? "OpenAPI" : "Markdown"}</span>
          <code>{isReference ? OPENAPI_URL : OVERVIEW_URL}</code>
        </div>
        {!isReference ? (
          <DocumentState state={overview}>
            <MarkdownDocument content={overview.data ?? ""} />
          </DocumentState>
        ) : (
          <DocumentState state={openApi}>
            <Suspense fallback={<div className="docs-loading" role="status"><span />API 목록을 준비하고 있습니다.</div>}>
              {openApi.data ? <ScalarReference document={openApi.data} theme={theme} /> : null}
            </Suspense>
          </DocumentState>
        )}
      </article>
    </section>
  );
}

function DocumentState({ children, state }: { children: ReactNode; state: { error: string; loading: boolean } }) {
  if (state.loading) return <div className="docs-loading" role="status"><span />문서를 준비하고 있습니다.</div>;
  if (state.error) return <div className="docs-error" role="alert">{state.error}<small>잠시 후 페이지를 다시 열어 주세요.</small></div>;
  return children;
}
