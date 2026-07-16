import { CaretRight } from "@phosphor-icons/react";
import { lazy, Suspense, useEffect, useMemo, useState, type ReactNode } from "react";

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
    return decodeURIComponent(rawHash);
  } catch {
    return rawHash;
  }
}

export default function DocsPage({ theme }: { theme: Theme }) {
  const [activeHash, setActiveHash] = useState(getDecodedHash);
  const [openGroups, setOpenGroups] = useState<Set<string>>(() => new Set());
  const overview = useOverviewDocument();
  const openApi = useOpenApiDocument();
  const markdownNavigation = useMemo(() => buildMarkdownNavigation(overview.data ?? ""), [overview.data]);
  const apiNavigation = useMemo(() => buildApiNavigation(openApi.data ?? {}), [openApi.data]);
  const isReference = activeHash.startsWith("docs/tag/");

  useEffect(() => {
    void import("./ScalarReference");
  }, []);

  useEffect(() => {
    const syncHash = () => setActiveHash(getDecodedHash());
    window.addEventListener("hashchange", syncHash);
    window.addEventListener("popstate", syncHash);
    return () => {
      window.removeEventListener("hashchange", syncHash);
      window.removeEventListener("popstate", syncHash);
    };
  }, []);

  useEffect(() => {
    const activeGroup = apiNavigation.find((group) => activeHash.startsWith(`${createTagAnchor(group)}/`));
    if (!activeGroup) return;
    setOpenGroups((current) => {
      if (current.has(activeGroup.slug)) return current;
      return new Set([...current, activeGroup.slug]);
    });
  }, [activeHash, apiNavigation]);

  useEffect(() => {
    const targetHash = activeHash || markdownNavigation[0]?.anchor;
    if (!targetHash) return;
    const timers: number[] = [];
    let attempts = 0;

    const scrollWhenReady = () => {
      const target = window.document.getElementById(targetHash);
      if (target) {
        target.scrollIntoView({ block: "start" });
        return;
      }
      if (attempts < 30) {
        attempts += 1;
        timers.push(window.setTimeout(scrollWhenReady, 120));
      }
    };

    timers.push(window.setTimeout(scrollWhenReady, 0));
    return () => timers.forEach((timer) => window.clearTimeout(timer));
  }, [activeHash, isReference, markdownNavigation]);

  const toggleGroup = (slug: string) => {
    setOpenGroups((current) => {
      const next = new Set(current);
      if (next.has(slug)) next.delete(slug);
      else next.add(slug);
      return next;
    });
  };

  const activeMarkdownAnchor = activeHash || markdownNavigation[0]?.anchor;

  return (
    <section className="docs-page" data-section={isReference ? "reference" : "overview"}>
      <aside aria-label="API 문서 메뉴">
        <p>API 문서</p>
        <nav className="docs-sidebar-nav">
          <div className="docs-overview-navigation">
            {markdownNavigation.map((item) => (
              <a
                aria-current={activeMarkdownAnchor === item.anchor ? "page" : undefined}
                href={`/docs#${encodeURIComponent(item.anchor)}`}
                key={item.anchor}
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
