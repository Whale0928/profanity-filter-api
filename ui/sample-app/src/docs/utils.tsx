import { Fragment, type ReactNode } from "react";

import { HTTP_METHODS } from "./constants";
import {
  type HttpMethod,
  type OperationObject,
  type OpenApiDocument,
  type OverviewLink,
  type SectionView,
} from "./types";

type RawOperation = {
  method: string;
  path: string;
  tagName: string;
  summary: string;
  operationId?: string;
};

export function buildSections(document: OpenApiDocument): SectionView[] {
  const operationsByTag = new Map<string, RawOperation[]>();

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

  const declaredTags: NonNullable<OpenApiDocument["tags"]> =
    document.tags ?? Array.from(operationsByTag.keys()).map((name) => ({ name }));

  return declaredTags
    .filter((tag) => operationsByTag.has(tag.name))
    .map((tag) => ({
      name: tag.name,
      slug: slugify(tag.name),
      description: tag.description,
      operations: (operationsByTag.get(tag.name) ?? []).map((operation) => ({
        method: operation.method as HttpMethod,
        path: operation.path,
        tagName: operation.tagName,
        summary: operation.summary,
        operationId: operation.operationId,
      })),
    }));
}

export function buildHashNavigation(content: string): OverviewLink[] {
  return parseMarkdownRootHeadings(content);
}

export function parseMarkdownRootHeadings(content: string): OverviewLink[] {
  const seen = new Map<string, number>();

  return content
    .replace(/\r\n/g, "\n")
    .split("\n")
    .map((line) => /^(#)\s+(.+)$/.exec(line))
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

export function createMarkdownHeadingId(value: string) {
  return `heading-${slugify(stripInlineMarkdown(value))}`;
}

export function createTagAnchor(section: Pick<SectionView, "name">) {
  return `docs/tag/${slugify(section.name)}`;
}

export function createOperationAnchor(operation: {
  method?: string;
  path?: string;
  tagName?: string;
  operationId?: string;
  summary?: string;
}) {
  return `docs/tag/${slugify(operation.tagName ?? "")}/${createScalarOperationSlug(operation)}`;
}

export function createScalarOperationSlug(operation: {
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

export function getSectionForHash(hash: string, sections: SectionView[]) {
  const [, tagSlug] = /^docs\/tag\/([^/]+)(?:\/.*)?$/.exec(hash) ?? [];
  if (!tagSlug) {
    return null;
  }
  return sections.find((section) => section.slug === tagSlug) ?? null;
}

export function parseMarkdown(content: string): ReactNode[] {
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

    if (isMarkdownHorizontalRule(line)) {
      nodes.push(<hr key={`hr-${index}`} />);
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
      !isMarkdownHorizontalRule(lines[index]) &&
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

export function isMarkdownHorizontalRule(line: string) {
  return /^\s{0,3}(?:-{3,}|\*{3,}|_{3,})\s*$/.test(line);
}

function renderTable(lines: string[], key: string) {
  const rows = lines
    .filter((line) => !/^\|\s*-.*/.test(line))
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

export function isHttpMethod(method: string): method is HttpMethod {
  return (HTTP_METHODS as readonly string[]).includes(method);
}

function isOperationObject(value: unknown): value is OperationObject {
  return Boolean(value && typeof value === "object" && ("responses" in (value as object) || "summary" in (value as object)));
}

function slugify(value: string) {
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
