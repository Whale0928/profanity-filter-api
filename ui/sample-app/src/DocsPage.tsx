import { ApiReferenceReact, type AnyApiReferenceConfiguration } from "@scalar/api-reference-react";
import "@scalar/api-reference-react/style.css";
import { Fragment, type ReactNode, useEffect, useMemo, useState } from "react";

const OPENAPI_DOCUMENT_PATH = "/openapi.json";
const OPENAPI_DOCUMENT_URL = `${OPENAPI_DOCUMENT_PATH}?v=local-docs`;
const OVERVIEW_MARKDOWN_PATH = "/overview.md";
const HTTP_METHODS = ["get", "post", "put", "patch", "delete", "options", "head", "trace"] as const;

const FALLBACK_OVERVIEW_MARKDOWN = `# Profanity Filter API

한국어 비속어 필터링 API는 문장 안의 부적절한 표현을 감지하고, 필요한 경우 검출된 단어를 마스킹해 반환합니다.

## 시작하기

1. 클라이언트 등록 API로 API Key를 발급합니다.
2. 인증이 필요한 요청은 \`x-api-key\` 헤더에 발급받은 API Key를 포함합니다.
3. \`/api/v1/filter\`에 검사할 \`text\`와 처리 방식인 \`mode\`를 전달합니다.

## 주요 기능

- 문장 안의 한국어와 영어 비속어를 검출합니다.
- 검출 결과를 목록으로 받거나, 검출된 단어를 \`*\`로 마스킹한 문장을 받을 수 있습니다.
- 실제 처리 결과는 응답 본문의 \`status.code\`와 \`status.message\`에서 확인합니다.
`;

type PagePath = "/" | `/docs${string}`;
type HttpMethod = (typeof HTTP_METHODS)[number];

type DocsPageProps = {
  currentPath: PagePath;
  onNavigate: (path: PagePath) => void;
};

type MarkdownState = {
  content: string;
  source: "local" | "fallback";
  url: string;
};

type OpenApiDocument = {
  openapi?: string;
  info?: {
    title?: string;
    version?: string;
    summary?: string;
    description?: string;
  };
  tags?: Array<{ name: string; description?: string }>;
  paths?: Record<string, Record<string, OperationObject | unknown>>;
  components?: unknown;
  servers?: unknown;
  security?: unknown;
};

type OperationObject = {
  tags?: string[];
  summary?: string;
  description?: string;
  operationId?: string;
  parameters?: unknown;
  requestBody?: unknown;
  responses?: unknown;
};

type OperationView = {
  method: HttpMethod;
  path: string;
  tagName: string;
  summary: string;
  operationId?: string;
};

type SectionView = {
  name: string;
  slug: string;
  description?: string;
  operations: OperationView[];
};

type OverviewLink = {
  title: string;
  anchor: string;
  level: number;
};

export default function DocsPage({ currentPath: _currentPath }: DocsPageProps) {
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

    fetch(OPENAPI_DOCUMENT_URL, { signal: controller.signal })
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
  const overviewLinks = useMemo(() => parseMarkdownHeadings(overview.content), [overview.content]);
  const selectedSection = useMemo(() => getSectionForHash(activeHash, sections), [activeHash, sections]);
  const isOverview = !selectedSection;
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
    await navigator.clipboard.writeText(OPENAPI_DOCUMENT_PATH);
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

function useMarkdownDocument(url: string, fallback: string): MarkdownState {
  const [state, setState] = useState<MarkdownState>({
    content: fallback,
    source: "fallback",
    url: "",
  });

  useEffect(() => {
    let active = true;

    async function loadMarkdown() {
      try {
        const response = await fetch(`${url}?v=${Date.now()}`);
        if (response.ok) {
          const content = await response.text();
          if (active && content.trim()) {
            setState({ content, source: "local", url });
            return;
          }
        }
      } catch {
        // Fallback content keeps the page readable when the local file is absent.
      }
      if (active) {
        setState({ content: fallback, source: "fallback", url: "" });
      }
    }

    loadMarkdown();
    return () => {
      active = false;
    };
  }, [fallback, url]);

  return state;
}

function buildSections(document: OpenApiDocument): SectionView[] {
  const operationsByTag = new Map<string, OperationView[]>();

  Object.entries(document.paths ?? {}).forEach(([path, methods]) => {
    Object.entries(methods).forEach(([method, operation]) => {
      if (!isHttpMethod(method) || !isOperationObject(operation)) {
        return;
      }
      const tagName = operation.tags?.[0] ?? "API";
      const operations = operationsByTag.get(tagName) ?? [];
      operations.push({
        method,
        path,
        tagName,
        summary: operation.summary ?? `${method.toUpperCase()} ${path}`,
        operationId: operation.operationId,
      });
      operationsByTag.set(tagName, operations);
    });
  });

  const declaredTags: Array<{ name: string; description?: string }> =
    document.tags ?? Array.from(operationsByTag.keys()).map((name) => ({ name }));

  const tagSections = declaredTags
    .filter((tag) => operationsByTag.has(tag.name))
    .map((tag) => ({
      name: tag.name,
      slug: slugify(tag.name),
      description: tag.description,
      operations: operationsByTag.get(tag.name) ?? [],
    }));

  return tagSections;
}

function createTagDocument(document: OpenApiDocument, tagName: string): OpenApiDocument {
  const filteredPaths = Object.fromEntries(
    Object.entries(document.paths ?? {})
      .map(([path, methods]) => {
        const filteredMethods = Object.fromEntries(
          Object.entries(methods).filter(([, operation]) =>
            isOperationObject(operation) ? operation.tags?.includes(tagName) : false,
          ),
        );
        return [path, filteredMethods];
      })
      .filter(([, methods]) => Object.keys(methods).length > 0),
  );

  return {
    openapi: document.openapi,
    info: document.info,
    servers: document.servers,
    security: document.security,
    tags: document.tags?.filter((tag) => tag.name === tagName),
    paths: filteredPaths,
    components: document.components,
  };
}

function isHttpMethod(method: string): method is HttpMethod {
  return HTTP_METHODS.includes(method as HttpMethod);
}

function isOperationObject(value: unknown): value is OperationObject {
  return Boolean(value && typeof value === "object" && ("responses" in value || "summary" in value));
}

function slugify(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/&/g, "and")
    .replace(/[^a-z0-9가-힣]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function parseMarkdownHeadings(content: string): OverviewLink[] {
  const seen = new Map<string, number>();

  return content
    .replace(/\r\n/g, "\n")
    .split("\n")
    .map((line) => /^(#{2,3})\s+(.+)$/.exec(line))
    .filter((match): match is RegExpExecArray => Boolean(match))
    .map((match) => {
      const title = stripInlineMarkdown(match[2]);
      const baseAnchor = createMarkdownHeadingId(title);
      const count = seen.get(baseAnchor) ?? 0;
      seen.set(baseAnchor, count + 1);
      return {
        title,
        anchor: count === 0 ? baseAnchor : `${baseAnchor}-${count}`,
        level: match[1].length,
      };
    });
}

function createMarkdownHeadingId(value: string) {
  return `heading-${slugify(stripInlineMarkdown(value))}`;
}

function stripInlineMarkdown(value: string) {
  return value.replace(/[`*_]/g, "").trim();
}

function createTagAnchor(section: Pick<SectionView, "name">) {
  return `docs/tag/${slugify(section.name)}`;
}

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

function getSectionForHash(hash: string, sections: SectionView[]) {
  const [, tagSlug] = /^docs\/tag\/([^/]+)(?:\/.*)?$/.exec(hash) ?? [];
  if (!tagSlug) {
    return null;
  }
  return sections.find((section) => section.slug === tagSlug) ?? null;
}

function createOperationAnchor(operation: {
  method?: string;
  path?: string;
  tagName?: string;
  operationId?: string;
  summary?: string;
}) {
  return `docs/tag/${slugify(operation.tagName ?? "")}/${createScalarOperationSlug(operation)}`;
}

function createScalarOperationSlug(operation: {
  method?: string;
  path?: string;
  operationId?: string;
  summary?: string;
}) {
  return [operation.method?.toLowerCase(), operation.path ?? operation.operationId ?? operation.summary ?? ""]
    .filter(Boolean)
    .join("-")
    .replace(/[{}]/g, "")
    .replace(/[^a-zA-Z0-9가-힣/_-]+/g, "-")
    .replace(/\//g, "-")
    .replace(/-+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function MarkdownArticle({ content }: { content: string }) {
  return <article className="markdown-article">{parseMarkdown(content)}</article>;
}

function parseMarkdown(content: string): ReactNode[] {
  const lines = content.replace(/\r\n/g, "\n").split("\n");
  const nodes: ReactNode[] = [];
  let index = 0;
  const headingIds = new Map<string, number>();

  while (index < lines.length) {
    const line = lines[index];

    if (!line.trim()) {
      index += 1;
      continue;
    }

    if (line.startsWith("```")) {
      const language = line.replace(/^```/, "").trim();
      const codeLines: string[] = [];
      index += 1;
      while (index < lines.length && !lines[index].startsWith("```")) {
        codeLines.push(lines[index]);
        index += 1;
      }
      index += 1;
      nodes.push(
        <pre key={`code-${index}`}>
          <code data-language={language || undefined}>{codeLines.join("\n")}</code>
        </pre>,
      );
      continue;
    }

    if (line.startsWith("|") && lines[index + 1]?.startsWith("|")) {
      const tableLines: string[] = [];
      while (index < lines.length && lines[index].startsWith("|")) {
        tableLines.push(lines[index]);
        index += 1;
      }
      nodes.push(renderTable(tableLines, `table-${index}`));
      continue;
    }

    const heading = /^(#{1,4})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      const children = renderInline(heading[2]);
      const baseId = createMarkdownHeadingId(heading[2]);
      const idCount = headingIds.get(baseId) ?? 0;
      headingIds.set(baseId, idCount + 1);
      const id = idCount === 0 ? baseId : `${baseId}-${idCount}`;
      if (level === 1) nodes.push(<h1 id={id} key={`h-${index}`}>{children}</h1>);
      if (level === 2) nodes.push(<h2 id={id} key={`h-${index}`}>{children}</h2>);
      if (level === 3) nodes.push(<h3 id={id} key={`h-${index}`}>{children}</h3>);
      if (level === 4) nodes.push(<h4 id={id} key={`h-${index}`}>{children}</h4>);
      index += 1;
      continue;
    }

    if (/^[-*]\s+/.test(line)) {
      const items: string[] = [];
      while (index < lines.length && /^[-*]\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^[-*]\s+/, ""));
        index += 1;
      }
      nodes.push(
        <ul key={`ul-${index}`}>
          {items.map((item, itemIndex) => (
            <li key={`${item}-${itemIndex}`}>{renderInline(item)}</li>
          ))}
        </ul>,
      );
      continue;
    }

    if (/^\d+\.\s+/.test(line)) {
      const items: string[] = [];
      while (index < lines.length && /^\d+\.\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^\d+\.\s+/, ""));
        index += 1;
      }
      nodes.push(
        <ol key={`ol-${index}`}>
          {items.map((item, itemIndex) => (
            <li key={`${item}-${itemIndex}`}>{renderInline(item)}</li>
          ))}
        </ol>,
      );
      continue;
    }

    const paragraph: string[] = [];
    while (
      index < lines.length &&
      lines[index].trim() &&
      !/^(#{1,4})\s+/.test(lines[index]) &&
      !/^[-*]\s+/.test(lines[index]) &&
      !/^\d+\.\s+/.test(lines[index]) &&
      !lines[index].startsWith("```") &&
      !lines[index].startsWith("|")
    ) {
      paragraph.push(lines[index].trim());
      index += 1;
    }
    nodes.push(<p key={`p-${index}`}>{renderInline(paragraph.join(" "))}</p>);
  }

  return nodes;
}

function renderTable(lines: string[], key: string) {
  const rows = lines
    .filter((line) => !/^\|\s*-/.test(line))
    .map((line) =>
      line
        .split("|")
        .slice(1, -1)
        .map((cell) => cell.trim()),
    );
  const [head, ...body] = rows;

  return (
    <div className="markdown-table-wrap" key={key}>
      <table>
        {head ? (
          <thead>
            <tr>
              {head.map((cell, cellIndex) => (
                <th key={`${cell}-${cellIndex}`}>{renderInline(cell)}</th>
              ))}
            </tr>
          </thead>
        ) : null}
        <tbody>
          {body.map((row, rowIndex) => (
            <tr key={`${row.join("|")}-${rowIndex}`}>
              {row.map((cell, cellIndex) => (
                <td key={`${cell}-${cellIndex}`}>{renderInline(cell)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function renderInline(text: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  const parts = text.split(/(`[^`]+`|\*\*[^*]+\*\*)/g).filter(Boolean);

  parts.forEach((part, index) => {
    if (part.startsWith("`") && part.endsWith("`")) {
      nodes.push(<code key={`${part}-${index}`}>{part.slice(1, -1)}</code>);
      return;
    }
    if (part.startsWith("**") && part.endsWith("**")) {
      nodes.push(<strong key={`${part}-${index}`}>{part.slice(2, -2)}</strong>);
      return;
    }
    nodes.push(<Fragment key={`${part}-${index}`}>{part}</Fragment>);
  });

  return nodes;
}
