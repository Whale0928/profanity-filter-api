import { ApiReferenceReact, type AnyApiReferenceConfiguration } from "@scalar/api-reference-react";
import "@scalar/api-reference-react/style.css";
import { useEffect, useMemo, useState } from "react";

import { OPENAPI_DOCUMENT_URL, OVERVIEW_MARKDOWN_PATH, FALLBACK_OVERVIEW_MARKDOWN } from "./docs/constants";
import type { OpenApiDocument } from "./docs/types";
import {
  buildSections,
  buildHashNavigation,
  createTagDocument,
  createOperationAnchor,
  createScalarOperationSlug,
  createTagAnchor,
  getSectionForHash,
  parseMarkdown,
} from "./docs/utils";
import { useMarkdownDocument } from "./docs/hooks";

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
  const [document, setDocument] = useState<OpenApiDocument | null>(null);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);
  const [activeHash, setActiveHash] = useState(getDecodedHash);
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

  useEffect(() => {
    const controller = new AbortController();

    fetch(`${OPENAPI_DOCUMENT_URL}?v=api-docs`, { signal: controller.signal })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`OpenAPI 문서를 불러오지 못했습니다. status=${response.status}`);
        }
        return response.json() as Promise<OpenApiDocument>;
      })
      .then((openApiDocument) => {
        setDocument(openApiDocument);
        setError("");
      })
      .catch((loadError: unknown) => {
        if (!controller.signal.aborted) {
          setError(loadError instanceof Error ? loadError.message : "OpenAPI 문서 로딩 실패");
        }
      });

    return () => controller.abort();
  }, []);

  const sections = useMemo(() => (document ? buildSections(document) : []), [document]);
  const overviewLinks = useMemo(() => buildHashNavigation(overview.content), [overview.content]);
  const selectedSection = useMemo(() => getSectionForHash(activeHash, sections), [activeHash, sections]);
  const referenceDocument = useMemo(() => {
    if (!document || !selectedSection) {
      return null;
    }
    return createTagDocument(document, selectedSection.name);
  }, [document, selectedSection]);

  const scalarConfiguration = useMemo<AnyApiReferenceConfiguration | null>(() => {
    if (!referenceDocument) {
      return null;
    }

    return {
      content: referenceDocument,
      title: referenceDocument.info?.title ?? "API 문서",
      slug: "docs",
      theme: "none",
      layout: "modern",
      forceDarkModeState: "light",
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
  }, [referenceDocument]);

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
      if (attempts < 10) {
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
        <div className="api-docs-loading">문서 로딩 중</div>
      </section>
    );
  }

  const isOverview = !selectedSection;

  return (
    <section className="api-docs-page" aria-label="API 문서">
      <aside className="api-docs-sidebar" aria-label="API 문서 메뉴">
        <nav className="api-docs-sidebar-nav">
          <div className="api-docs-nav-item">
            <a href="/docs#overview">
              <span>Overview</span>
            </a>
            <div className="api-docs-sidebar-children" aria-label="Overview 섹션">
              {overviewLinks.map((link) => (
                <a
                  className={link.level > 2 ? "nested" : undefined}
                  href={`/docs#${encodeURIComponent(link.anchor)}`}
                  key={link.anchor}
                >
                  <span>{link.title}</span>
                </a>
              ))}
            </div>
          </div>

          {sections.map((section) => (
            <div className="api-docs-nav-item" key={section.slug}>
              <a href={`/docs#${encodeURIComponent(createTagAnchor(section))}`}>
                <span>{section.name}</span>
              </a>
              {section.operations.length > 0 ? (
                <div className="api-docs-sidebar-children" aria-label={`${section.name} 엔드포인트`}>
                  {section.operations.map((operation) => (
                    <a
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
