import { ApiReferenceReact, type AnyApiReferenceConfiguration } from "@scalar/api-reference-react";
import "@scalar/api-reference-react/style.css";
import { useEffect, useMemo, useState } from "react";
import { parse } from "yaml";

const OPENAPI_DOCUMENT_URL = "/openapi.yaml";

const HTTP_METHODS = ["get", "post", "put", "patch", "delete", "options", "head", "trace"] as const;

type PagePath = "/" | `/docs${string}`;
type HttpMethod = (typeof HTTP_METHODS)[number];

type OpenApiDocument = {
  openapi?: string;
  info?: {
    title?: string;
    version?: string;
    summary?: string;
    description?: string;
  };
  tags?: Array<{ name: string; description?: string }>;
  "x-tagGroups"?: Array<{ name: string; tags: string[] }>;
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
  type: "overview" | "tag";
  name: string;
  slug: string;
  description?: string;
  operations: OperationView[];
};

type NavigationGroup = {
  name: string;
  sections: SectionView[];
};

type DocsPageProps = {
  currentPath: PagePath;
  onNavigate: (path: PagePath) => void;
};

export default function DocsPage({ currentPath, onNavigate }: DocsPageProps) {
  const [document, setDocument] = useState<OpenApiDocument | null>(null);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    fetch(OPENAPI_DOCUMENT_URL, { signal: controller.signal })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`OpenAPI 문서를 불러오지 못했습니다. status=${response.status}`);
        }
        return response.text();
      })
      .then((text) => {
        setDocument(parse(text) as OpenApiDocument);
        setError("");
      })
      .catch((loadError: unknown) => {
        if (!controller.signal.aborted) {
          setError(loadError instanceof Error ? loadError.message : "OpenAPI 문서 로딩 실패");
        }
      });

    return () => controller.abort();
  }, []);

  const { groups, sections } = useMemo(() => {
    if (!document) {
      return { groups: [], sections: [] };
    }
    return buildNavigation(document);
  }, [document]);

  const selectedSlug = getSelectedSlug(currentPath);
  const selectedSection = sections.find((section) => section.slug === selectedSlug) ?? sections[0];
  const isOverview = selectedSection?.type === "overview";
  const referenceDocument = useMemo(() => {
    if (!document) {
      return null;
    }
    return selectedSection?.type === "tag" ? createTagDocument(document, selectedSection.name) : document;
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
        createOperationAnchor({ method, path, operationId, summary }),
    };
  }, [referenceDocument]);

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

  if (!document || !selectedSection || !scalarConfiguration) {
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
          {groups.map((group) => (
            <div className="api-docs-nav-group" key={group.name}>
              <strong>{group.name}</strong>
              {group.sections.map((section) => (
                <div className="api-docs-nav-item" key={section.slug}>
                  <a
                    aria-current={section.slug === selectedSection.slug ? "page" : undefined}
                    className={section.slug === selectedSection.slug ? "active" : ""}
                    href={section.type === "overview" ? "/docs" : `/docs/${section.slug}`}
                    onClick={(event) => {
                      event.preventDefault();
                      onNavigate(section.type === "overview" ? "/docs" : (`/docs/${section.slug}` as PagePath));
                    }}
                  >
                    <span>{section.name}</span>
                    {section.type === "tag" ? <small>{section.operations.length}</small> : null}
                  </a>
                  {section.slug === selectedSection.slug && section.operations.length > 0 ? (
                    <div className="api-docs-sidebar-children" aria-label={`${section.name} 엔드포인트`}>
                      {section.operations.map((operation) => (
                        <a href={`#${createOperationAnchor(operation)}`} key={`${operation.method}-${operation.path}`}>
                          <span>{operation.summary}</span>
                        </a>
                      ))}
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          ))}
        </nav>
      </aside>

      <div className="api-docs-content">
        <div className="api-docs-copy-actions" aria-label="문서 액션">
          <span>{document.info?.version ?? "0.0.0"}</span>
          <span>{document.openapi ?? "OpenAPI"}</span>
          <button onClick={copyOpenApiUrl} type="button">
            {copied ? "복사됨" : "OpenAPI 경로 복사"}
          </button>
        </div>

        <main className="api-docs-main">
          <header className={`api-docs-hero${isOverview ? "" : " compact"}`}>
            <p>Open API 가이드</p>
            <h1>{isOverview ? "말조심하세욧 API" : selectedSection.name}</h1>
            {isOverview ? (
              <strong>{document.info?.summary ?? "한국어 입력 검수를 위한 REST API입니다."}</strong>
            ) : selectedSection.description ? (
              <MarkdownText text={selectedSection.description} />
            ) : null}
          </header>

          {isOverview ? <Overview document={document} sections={sections} /> : null}

          <div className="api-docs-reference" data-api-reference-root="true">
            <ApiReferenceReact configuration={scalarConfiguration} />
          </div>
        </main>
      </div>
    </section>
  );
}

function Overview({ document, sections }: { document: OpenApiDocument; sections: SectionView[] }) {
  const tags = sections.filter((section) => section.type === "tag");
  const operationCount = tags.reduce((sum, section) => sum + section.operations.length, 0);

  return (
    <section className="api-docs-overview" aria-label="API 개요">
      <div className="api-docs-overview-stats">
        <span>{tags.length} tags</span>
        <span>{operationCount} operations</span>
        <span>{document.servers ? "server defined" : "server pending"}</span>
      </div>
      <MarkdownText text={document.info?.description ?? ""} />
    </section>
  );
}

function MarkdownText({ text }: { text: string }) {
  const blocks = text
    .split(/\n{2,}/)
    .map((block) => block.trim())
    .filter(Boolean);

  if (blocks.length === 0) {
    return null;
  }

  return (
    <div className="api-docs-markdown">
      {blocks.map((block) => {
        if (block.startsWith("## ")) {
          return <h2 key={block}>{block.replace(/^##\s+/, "")}</h2>;
        }
        if (block.startsWith("### ")) {
          return <h3 key={block}>{block.replace(/^###\s+/, "")}</h3>;
        }

        const lines = block.split("\n").map((line) => line.trim());
        const listItems = toListItems(lines);

        if (listItems.length > 0) {
          return (
            <ul key={block}>
              {listItems.map((line) => (
                <li key={line}>{renderInline(line)}</li>
              ))}
            </ul>
          );
        }

        return <p key={block}>{renderInline(lines.join(" "))}</p>;
      })}
    </div>
  );
}

function buildNavigation(document: OpenApiDocument) {
  const tagMap = new Map((document.tags ?? []).map((tag) => [tag.name, tag.description]));
  const operationMap = new Map<string, OperationView[]>();

  Object.entries(document.paths ?? {}).forEach(([path, pathItem]) => {
    HTTP_METHODS.forEach((method) => {
      const operation = pathItem?.[method];
      if (!isOperationObject(operation)) {
        return;
      }

      const tagName = operation.tags?.[0] ?? "Default";
      const operations = operationMap.get(tagName) ?? [];
      operations.push({
        method,
        path,
        tagName,
        summary: operation.summary ?? `${method.toUpperCase()} ${path}`,
        operationId: operation.operationId,
      });
      operationMap.set(tagName, operations);
    });
  });

  const tagSections = Array.from(operationMap.entries()).map<SectionView>(([name, operations]) => ({
    type: "tag",
    name,
    slug: slugify(name),
    description: tagMap.get(name),
    operations,
  }));
  const byTag = new Map(tagSections.map((section) => [section.name, section]));
  const usedTags = new Set<string>();

  const groups: NavigationGroup[] = [
    {
      name: "시작하기",
      sections: [
        {
          type: "overview",
          name: "API 소개",
          slug: "introduction",
          description: document.info?.summary,
          operations: [],
        },
      ],
    },
  ];

  (document["x-tagGroups"] ?? []).forEach((tagGroup) => {
    const sections = tagGroup.tags
      .map((tagName) => byTag.get(tagName))
      .filter((section): section is SectionView => Boolean(section));

    sections.forEach((section) => usedTags.add(section.name));

    if (tagGroup.name === "시작하기") {
      groups[0].sections.push(...sections);
      return;
    }

    if (sections.length > 0) {
      groups.push({ name: tagGroup.name, sections });
    }
  });

  const ungrouped = tagSections.filter((section) => !usedTags.has(section.name));
  if (ungrouped.length > 0) {
    groups.push({ name: "기타", sections: ungrouped });
  }

  return { groups, sections: groups.flatMap((group) => group.sections) };
}

function createTagDocument(document: OpenApiDocument, tagName: string): OpenApiDocument {
  const paths = Object.entries(document.paths ?? {}).reduce<NonNullable<OpenApiDocument["paths"]>>(
    (nextPaths, [path, pathItem]) => {
      const nextPathItem = Object.entries(pathItem).reduce<Record<string, OperationObject | unknown>>(
        (nextItem, [method, operation]) => {
          if (HTTP_METHODS.includes(method as HttpMethod) && isOperationObject(operation)) {
            if (operation.tags?.includes(tagName)) {
              nextItem[method] = operation;
            }
            return nextItem;
          }
          nextItem[method] = operation;
          return nextItem;
        },
        {},
      );

      if (Object.keys(nextPathItem).some((key) => HTTP_METHODS.includes(key as HttpMethod))) {
        nextPaths[path] = nextPathItem;
      }
      return nextPaths;
    },
    {},
  );

  return {
    ...document,
    info: {
      ...document.info,
      title: tagName,
      description: document.tags?.find((tag) => tag.name === tagName)?.description ?? document.info?.description,
    },
    tags: document.tags?.filter((tag) => tag.name === tagName),
    "x-tagGroups": undefined,
    paths,
  };
}

function getSelectedSlug(path: string) {
  return decodeURIComponent(path.replace(/^\/docs\/?/, "").split("/")[0] ?? "") || "introduction";
}

function toListItems(lines: string[]) {
  if (!lines.some((line) => line.startsWith("- "))) {
    return [];
  }

  return lines.reduce<string[]>((items, line) => {
    if (line.startsWith("- ")) {
      items.push(line.replace(/^-\s+/, ""));
      return items;
    }

    if (items.length > 0) {
      items[items.length - 1] = `${items[items.length - 1]} ${line}`.trim();
    }

    return items;
  }, []);
}

function renderInline(text: string) {
  return text.split(/(`[^`]+`)/g).map((part, index) => {
    if (part.startsWith("`") && part.endsWith("`")) {
      return <code key={`${part}-${index}`}>{part.slice(1, -1)}</code>;
    }
    return part;
  });
}

function isOperationObject(value: unknown): value is OperationObject {
  return Boolean(value && typeof value === "object" && ("responses" in value || "operationId" in value || "summary" in value));
}

function slugify(value: string) {
  return (
    value
      .trim()
      .toLowerCase()
      .replace(/&/g, "and")
      .replace(/[^a-z0-9가-힣]+/g, "-")
      .replace(/^-+|-+$/g, "") || encodeURIComponent(value.trim())
  );
}

function createOperationAnchor(input: { method: string; path: string; operationId?: string; summary?: string }) {
  return slugify(input.operationId ?? input.summary ?? `${input.method}-${input.path}`);
}
