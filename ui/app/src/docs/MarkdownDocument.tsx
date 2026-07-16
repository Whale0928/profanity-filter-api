import { Fragment, type ReactNode } from "react";

import { createMarkdownHeadingId } from "./navigation";

export default function MarkdownDocument({ content }: { content: string }) {
  return <div className="markdown-document">{parseMarkdown(content)}</div>;
}

export function extractMarkdownSection(content: string, title: string) {
  const lines = content.replace(/\r\n/g, "\n").split("\n");
  const start = lines.findIndex((line) => line.trim() === `# ${title}`);
  if (start < 0) return "";
  const end = lines.findIndex((line, index) => index > start && /^#\s+/.test(line.trim()));
  return lines.slice(start, end < 0 ? undefined : end).join("\n");
}

function parseMarkdown(content: string): ReactNode[] {
  const lines = content.replace(/\r\n/g, "\n").split("\n");
  const nodes: ReactNode[] = [];
  let index = 0;
  const headingIds = new Map<string, number>();

  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim()) { index += 1; continue; }

    if (/^\s{0,3}(?:-{3,}|\*{3,}|_{3,})\s*$/.test(line)) {
      nodes.push(<hr key={`hr-${index}`} />);
      index += 1;
      continue;
    }

    if (line.startsWith("```")) {
      const language = line.slice(3).trim();
      const codeLines: string[] = [];
      index += 1;
      while (index < lines.length && !lines[index].startsWith("```")) {
        codeLines.push(lines[index]);
        index += 1;
      }
      index += 1;
      nodes.push(<pre key={`code-${index}`}><code data-language={language || undefined}>{codeLines.join("\n")}</code></pre>);
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
      const count = headingIds.get(baseId) ?? 0;
      headingIds.set(baseId, count + 1);
      const id = count === 0 ? baseId : `${baseId}-${count}`;
      if (level === 1) nodes.push(<h1 id={id} key={`h-${index}`}>{children}</h1>);
      else if (level === 2) nodes.push(<h2 id={id} key={`h-${index}`}>{children}</h2>);
      else if (level === 3) nodes.push(<h3 id={id} key={`h-${index}`}>{children}</h3>);
      else nodes.push(<h4 id={id} key={`h-${index}`}>{children}</h4>);
      index += 1;
      continue;
    }

    if (/^[-*]\s+/.test(line)) {
      const items: string[] = [];
      while (index < lines.length && /^[-*]\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^[-*]\s+/, ""));
        index += 1;
      }
      nodes.push(<ul key={`ul-${index}`}>{items.map((item, itemIndex) => <li key={`${item}-${itemIndex}`}>{renderInline(item)}</li>)}</ul>);
      continue;
    }

    if (/^\d+\.\s+/.test(line)) {
      const items: string[] = [];
      while (index < lines.length && /^\d+\.\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^\d+\.\s+/, ""));
        index += 1;
      }
      nodes.push(<ol key={`ol-${index}`}>{items.map((item, itemIndex) => <li key={`${item}-${itemIndex}`}>{renderInline(item)}</li>)}</ol>);
      continue;
    }

    const paragraph: string[] = [];
    while (
      index < lines.length && lines[index].trim() &&
      !/^(#{1,4})\s+/.test(lines[index]) && !/^[-*]\s+/.test(lines[index]) &&
      !/^\d+\.\s+/.test(lines[index]) && !lines[index].startsWith("```") &&
      !lines[index].startsWith("|") && !/^\s{0,3}(?:-{3,}|\*{3,}|_{3,})\s*$/.test(lines[index])
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
    .map((line) => line.split("|").slice(1, -1).map((cell) => cell.trim()));
  const [head, ...body] = rows;
  return (
    <div className="markdown-table-wrap" key={key}>
      <table>
        {head ? <thead><tr>{head.map((cell, index) => <th key={`${cell}-${index}`}>{renderInline(cell)}</th>)}</tr></thead> : null}
        <tbody>{body.map((row, rowIndex) => <tr key={`${row.join("|")}-${rowIndex}`}>{row.map((cell, cellIndex) => <td key={`${cell}-${cellIndex}`}>{renderInline(cell)}</td>)}</tr>)}</tbody>
      </table>
    </div>
  );
}

function renderInline(text: string): ReactNode[] {
  return text.split(/(`[^`]+`|\*\*[^*]+\*\*)/g).filter(Boolean).map((part, index) => {
    if (part.startsWith("`") && part.endsWith("`")) return <code key={`${part}-${index}`}>{part.slice(1, -1)}</code>;
    if (part.startsWith("**") && part.endsWith("**")) return <strong key={`${part}-${index}`}>{part.slice(2, -2)}</strong>;
    return <Fragment key={`${part}-${index}`}>{part}</Fragment>;
  });
}
