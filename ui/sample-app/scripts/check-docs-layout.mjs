import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { parse } from "yaml";

const root = resolve(import.meta.dirname, "..", "..");
const appRoot = resolve(import.meta.dirname, "..");

const openapi = parse(readFileSync(resolve(root, "openapi.yaml"), "utf8"));
const app = readFileSync(resolve(appRoot, "src", "App.tsx"), "utf8");
const docsPage = readFileSync(resolve(appRoot, "src", "DocsPage.tsx"), "utf8");
const guidelines = readFileSync(resolve(appRoot, "DESIGN_GUIDELINES.md"), "utf8");
const styles = readFileSync(resolve(appRoot, "src", "styles.css"), "utf8");

assert.deepEqual(
  openapi["x-tagGroups"]?.map((group) => group.name),
  ["시작하기", "문서/예제", "운영"],
  "OpenAPI root must define Toss-like x-tagGroups.",
);

assert.match(docsPage, /ApiReferenceReact/, "DocsPage must render the OpenAPI body with Scalar.");
assert.match(docsPage, /showSidebar:\s*false/, "Scalar sidebar must be hidden in favor of the shell sidebar.");
assert.match(docsPage, /hideTestRequestButton:\s*true/, "Interactive request execution must stay hidden.");
assert.match(docsPage, /hideClientButton:\s*true/, "Scalar client button must stay hidden.");
assert.match(docsPage, /documentDownloadType:\s*"none"/, "Scalar document download UI must stay hidden.");

const sidebarBlock = extractRule(styles, ".api-docs-sidebar");
assert.match(sidebarBlock, /position:\s*sticky/, "Docs sidebar must remain sticky, not fixed overlay.");
assert.match(sidebarBlock, /top:\s*var\(--docs-header-height\)/, "Docs sidebar must respect header height.");
assert.match(sidebarBlock, /max-height:\s*calc\(100dvh - var\(--docs-header-height\)\)/, "Docs sidebar must be capped to viewport height.");
assert.match(sidebarBlock, /overflow-y:\s*auto/, "Docs sidebar must scroll internally.");
assert.match(sidebarBlock, /overscroll-behavior:\s*contain/, "Docs sidebar must not hijack page drag at scroll edges.");

const mobileBlock = extractMedia(styles, "@media (max-width: 767px)");
assert.match(mobileBlock, /\.api-docs-page\s*{[^}]*grid-template-columns:\s*minmax\(0, 1fr\)/s, "Mobile docs layout must use one column.");
assert.match(mobileBlock, /\.api-docs-sidebar\s*{[^}]*position:\s*static/s, "Mobile sidebar must remain in normal document flow.");
assert.match(mobileBlock, /\.api-docs-sidebar\s*{[^}]*max-height:\s*42dvh/s, "Mobile sidebar must not cover the viewport.");
assert.doesNotMatch(mobileBlock, /\.api-docs-sidebar\s*{[^}]*position:\s*fixed/s, "Mobile sidebar must not become a fixed overlay.");
assert.doesNotMatch(mobileBlock, /\.api-docs-sidebar\s*{[^}]*position:\s*sticky/s, "Mobile sidebar must not stick over content while dragging.");

assert.match(guidelines, /첫 화면은 4개 블록으로 구성한다\./, "Guidelines must define the four-block landing structure.");
assert.match(guidelines, /기능 설명이 확정되기 전까지 2번, 3번 블록 내부 텍스트는 블록 순서만 표시한다\./, "Guidelines must keep pending landing copy as block-order placeholders.");
assert.match(app, /title:\s*"2번 블럭"/, "Second landing block must use placeholder copy only.");
assert.match(app, /title:\s*"3번 블럭"/, "Third landing block must use placeholder copy only.");
assert.match(app, /신청하기/, "Footer CTA must include an apply button.");
assert.match(app, /문서 보기/, "Footer CTA must include a docs button.");
assert.doesNotMatch(app, /댓글은 짧게|이름은 더 조심|긴 글도 기준은 단순하게|작게 시작합니다|구현 규칙은 문서에서 봅니다/, "Pending landing sections must not keep functional explanation copy.");

function extractRule(source, selector) {
  const start = source.indexOf(`${selector} {`);
  assert.notEqual(start, -1, `${selector} rule not found.`);
  const end = source.indexOf("}", start);
  assert.notEqual(end, -1, `${selector} rule is not closed.`);
  return source.slice(start, end + 1);
}

function extractMedia(source, media) {
  const start = source.indexOf(media);
  assert.notEqual(start, -1, `${media} block not found.`);
  const next = source.indexOf("@media", start + media.length);
  return next === -1 ? source.slice(start) : source.slice(start, next);
}
