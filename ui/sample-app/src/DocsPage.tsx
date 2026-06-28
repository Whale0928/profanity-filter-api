import { ApiReferenceReact, type AnyApiReferenceConfiguration } from "@scalar/api-reference-react";
import "@scalar/api-reference-react/style.css";
import { useEffect, useMemo, useState } from "react";

import { OPENAPI_DOCUMENT_URL, OVERVIEW_MARKDOWN_PATH, FALLBACK_OVERVIEW_MARKDOWN } from "./docs/constants";
import {
  buildSections,
  buildHashNavigation,
  createOperationAnchor,
  createScalarOperationSlug,
  createTagAnchor,
  getSectionForHash,
  parseMarkdown,
} from "./docs/utils";
import { useMarkdownDocument, useOpenApiDocument } from "./docs/hooks";

function getDecodedHash() {
  const rawHash = window.location.hash.replace(/^#/, "");
  if (!rawHash) {
    return "";
  }
  try {
    return decodeURIComponent(rawHash);
  } catch {
    return rawHash;
  }
}

export default function DocsPage() {
  const [copied, setCopied] = useState(false);
  const [activeHash, setActiveHash] = useState(getDecodedHash);
  const [openSectionSlugs, setOpenSectionSlugs] = useState<Set<string>>(() => new Set());
  const { document, error } = useOpenApiDocument(OPENAPI_DOCUMENT_URL);
  const overview = useMarkdownDocument(OVERVIEW_MARKDOWN_PATH, FALLBACK_OVERVIEW_MARKDOWN);

  useEffect(() => {
    const syncHash = () => setActiveHash(getDecodedHash());
    window.addEventListener("hashchange", syncHash);
    window.addEventListener("popstate", syncHash);
    return () => {
      window.removeEventListener("hashchange", syncHash);
      window.removeEventListener("popstate", syncHash);
    };
  }, []);

  const sections = useMemo(() => (document ? buildSections(document) : []), [document]);
  const overviewLinks = useMemo(() => buildHashNavigation(overview.content), [overview.content]);
  const selectedSection = useMemo(() => getSectionForHash(activeHash, sections), [activeHash, sections]);

  const scalarConfiguration = useMemo<AnyApiReferenceConfiguration | null>(() => {
    if (!document) {
      return null;
    }

    return {
      content: document,
      title: document.info?.title ?? "API 문서",
      slug: "docs",
      theme: "none",
      layout: "modern",
      forceDarkModeState: "light",
      defaultOpenAllTags: true,
      defaultOpenFirstTag: false,
      hideDarkModeToggle: true,
      documentDownloadType: "none",
      hideTestRequestButton: true,
      hideClientButton: true,
      hideModels: true,
      hideSearch: true,
      showSidebar: false,
      showDeveloperTools: "never",
      searchHotKey: "k",
      withDefaultFonts: false,
      agent: { disabled: true },
      mcp: { disabled: true },
      generateTagSlug: ({ name }) => slugify(name ?? ""),
      generateOperationSlug: ({ method, path, operationId, summary }) =>
        createScalarOperationSlug({ method, path, operationId, summary }),
    };
  }, [document]);

  useEffect(() => {
    if (!selectedSection) {
      return;
    }
    setOpenSectionSlugs((current) => {
      if (current.has(selectedSection.slug)) {
        return current;
      }
      const next = new Set(current);
      next.add(selectedSection.slug);
      return next;
    });
  }, [selectedSection]);

  useEffect(() => {
    const targetHash = activeHash || "overview";
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
  }, [activeHash, selectedSection?.slug, overview.content]);

  const copyOpenApiUrl = async () => {
    if (!navigator.clipboard) {
      return;
    }
    await navigator.clipboard.writeText(OPENAPI_DOCUMENT_URL);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1400);
  };

  if (error) {
    return (
      <section className="api-docs-page" aria-label="API 문서">
        <div className="api-docs-error">{error}</div>
      </section>
    );
  }

  if (!document) {
    return (
      <section className="api-docs-page" aria-label="API 문서" role="status">
        <DocsLoadingFrame />
      </section>
    );
  }

  const isOverview = !activeHash.startsWith("docs/tag/");
  const getLinkClassName = (anchor: string, extraClassName?: string) =>
    [activeHash === anchor ? "active" : "", extraClassName].filter(Boolean).join(" ") || undefined;
  const toggleOpenApiSection = (sectionSlug: string) => {
    setOpenSectionSlugs((current) => {
      const next = new Set(current);
      if (next.has(sectionSlug)) {
        next.delete(sectionSlug);
      } else {
        next.add(sectionSlug);
      }
      return next;
    });
  };

  return (
    <section className="api-docs-page" aria-label="API 문서">
      <aside className="api-docs-sidebar" aria-label="API 문서 메뉴">
        <nav className="api-docs-sidebar-nav">
          <div className="api-docs-root-group">
            <p className="api-docs-root-label">Overview</p>
            <div className="api-docs-sidebar-children" aria-label="Overview 섹션">
              {overviewLinks.map((link) => (
                <a
                  className={getLinkClassName(link.anchor, link.level > 1 ? "nested" : undefined)}
                  href={`/docs#${encodeURIComponent(link.anchor)}`}
                  key={link.anchor}
                >
                  <span>{link.title}</span>
                </a>
              ))}
            </div>
          </div>

          <div className="api-docs-root-group">
            <p className="api-docs-root-label">OpenAPI</p>
            {sections.map((section) => (
              <div className="api-docs-nav-item" key={section.slug}>
                <button
                  aria-controls={`openapi-section-${section.slug}`}
                  aria-expanded={openSectionSlugs.has(section.slug)}
                  className={getLinkClassName(createTagAnchor(section), "api-docs-section-toggle")}
                  onClick={() => toggleOpenApiSection(section.slug)}
                  type="button"
                >
                  <span>{section.name}</span>
                </button>
                {openSectionSlugs.has(section.slug) && section.operations.length > 0 ? (
                  <div
                    className="api-docs-sidebar-children"
                    id={`openapi-section-${section.slug}`}
                    aria-label={`${section.name} 엔드포인트`}
                  >
                    {section.operations.map((operation) => (
                      <a
                        className={getLinkClassName(createOperationAnchor(operation))}
                        href={`/docs#${encodeURIComponent(createOperationAnchor(operation))}`}
                        key={`${operation.method}-${operation.path}`}
                      >
                        <span>{operation.summary}</span>
                      </a>
                    ))}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </nav>
      </aside>

      <div className="api-docs-content">
        <div className="api-docs-copy-actions" aria-label="문서 액션">
          <span>{isOverview ? (overview.source === "local" ? "Markdown" : "fallback") : document.openapi}</span>
          <button onClick={copyOpenApiUrl} type="button">
            {copied ? "복사됨" : "OpenAPI 경로 복사"}
          </button>
        </div>

        <main className="api-docs-main">
          {isOverview ? (
            <section className="api-docs-overview" id="overview" aria-label="API 개요">
              <p className="docs-source-label">
                Overview
                <span>{overview.source === "local" ? overview.url : "fallback"}</span>
              </p>
              <MarkdownArticle content={overview.content} />
            </section>
          ) : (
            <div className="api-docs-reference" data-api-reference-root="true">
              {scalarConfiguration ? <ApiReferenceReact configuration={scalarConfiguration} /> : null}
            </div>
          )}
        </main>
      </div>
    </section>
  );
}

function DocsLoadingFrame() {
  return (
    <div className="docs-loading-overlay" aria-label="문서 로딩" role="status">
      <DocsLoadingBar />
    </div>
  );
}

function DocsLoadingBar() {
  return (
    <div className="docs-loading-bar">
      <span />
    </div>
  );
}

function MarkdownArticle({ content }: { content: string }) {
  return <article className="docs-markdown-article">{parseMarkdown(content)}</article>;
}

function slugify(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/&/g, "and")
    .replace(/[^a-z0-9가-힣]+/g, "-")
    .replace(/^-+|-+$/g, "");
}
