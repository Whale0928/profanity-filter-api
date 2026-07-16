import type {
  ApiGroupNavigation,
  ApiOperationNavigation,
  MarkdownNavigation,
  OpenApiDocument,
  OpenApiOperation,
} from "./types";

const HTTP_METHODS = new Set(["get", "post", "put", "patch", "delete", "options", "head", "trace"]);

export function buildMarkdownNavigation(content: string): MarkdownNavigation[] {
  const seen = new Map<string, number>();
  return content
    .replace(/\r\n/g, "\n")
    .split("\n")
    .map((line) => /^#\s+(.+)$/.exec(line))
    .filter((match): match is RegExpExecArray => Boolean(match))
    .map((match) => {
      const title = stripInlineMarkdown(match[1]);
      const baseAnchor = createMarkdownHeadingId(title);
      const count = seen.get(baseAnchor) ?? 0;
      seen.set(baseAnchor, count + 1);
      return { title, anchor: count === 0 ? baseAnchor : `${baseAnchor}-${count}` };
    });
}

export function buildApiNavigation(document: OpenApiDocument): ApiGroupNavigation[] {
  const operationsByTag = new Map<string, ApiOperationNavigation[]>();

  Object.entries(document.paths ?? {}).forEach(([path, methods]) => {
    Object.entries(methods).forEach(([method, value]) => {
      if (!HTTP_METHODS.has(method) || !isOperation(value)) return;
      const tag = value.tags?.[0] ?? "API";
      const operation = {
        method,
        path,
        slug: createOperationSlug({ method, path, operationId: value.operationId, summary: value.summary }),
        summary: value.summary ?? `${method.toUpperCase()} ${path}`,
        tag,
      };
      operationsByTag.set(tag, [...(operationsByTag.get(tag) ?? []), operation]);
    });
  });

  const declaredTags = document.tags?.map((tag) => tag.name) ?? Array.from(operationsByTag.keys());
  return declaredTags
    .filter((name) => operationsByTag.has(name))
    .map((name) => ({ name, slug: slugify(name), operations: operationsByTag.get(name) ?? [] }));
}

export function createMarkdownHeadingId(value: string) {
  return `heading-${slugify(stripInlineMarkdown(value))}`;
}

export function createTagAnchor(group: Pick<ApiGroupNavigation, "slug">) {
  return `docs/tag/${group.slug}`;
}

export function createOperationAnchor(operation: Pick<ApiOperationNavigation, "slug" | "tag">) {
  return `docs/tag/${slugify(operation.tag)}/${operation.slug}`;
}

export function createOperationSlug(operation: {
  method?: string;
  operationId?: string;
  path?: string;
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

export function slugify(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/&/g, "and")
    .replace(/[^a-z0-9가-힣]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function stripInlineMarkdown(value: string) {
  return value.replace(/[`*_]/g, "").trim();
}

function isOperation(value: unknown): value is OpenApiOperation {
  return Boolean(value && typeof value === "object" && ("responses" in value || "summary" in value));
}
