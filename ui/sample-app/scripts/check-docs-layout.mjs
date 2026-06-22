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
assert.match(guidelines, /2번 블록은 프로젝트 정체성을 짧게 설명한다\./, "Guidelines must define the second block as the project identity section.");
assert.match(guidelines, /3번 블록은 사용 시나리오를 영상형 흐름으로 보여준다\./, "Guidelines must define the third block as the scenario reel section.");
assert.match(styles, /--landing-letterbox:\s*16vw/, "Landing sections must define a ratio-based side letterbox gutter.");
assert.match(styles, /--landing-rail-width:\s*68vw/, "Landing sections must use a ratio-based readable rail.");
assert.match(styles, /--landing-section-height:\s*calc\(100dvh - var\(--landing-nav-height\)\)/, "Landing story sections must target one viewport below the fixed nav.");
assert.doesNotMatch(styles, /--landing-rail-width:\s*\d+px/, "Landing rail must not use a fixed pixel width.");
assert.match(styles, /calc\(100vw - var\(--landing-letterbox\) - var\(--landing-letterbox\)\)/, "Landing sections must reserve side letterbox space.");
assert.match(styles, /\.story-section\s*{[^}]*min-height:\s*var\(--landing-section-height\)/s, "Second landing block must fit inside the viewport section height.");
assert.match(styles, /\.start-section\s*{[^}]*min-height:\s*var\(--landing-section-height\)/s, "Third landing block must fit inside the viewport section height.");
assert.match(styles, /\.story-section,[\s\S]*?\.start-section,[\s\S]*?\.footer-cta\s*{[^}]*scroll-margin-top:\s*var\(--landing-nav-height\)/s, "Landing sections must account for the fixed nav when anchored.");
assert.doesNotMatch(styles, /scroll-snap-type:\s*y/, "Landing pages must not force scroll snapping between sections.");
assert.doesNotMatch(styles, /\.story-block\s*{[^}]*680px/s, "Story block must not rely on a fixed pixel minimum height.");
assert.match(app, /한국어 문장을 API로 필터링합니다/, "Second landing block must explain what the project does.");
assert.match(app, /role="tablist"/, "Second landing block must expose type options as tabs.");
assert.match(app, /무료\/실용/, "Second landing block must include the free practical positioning tab.");
assert.match(app, /한국어 중심/, "Second landing block must include the Korean-focused positioning tab.");
assert.match(app, /개발자 연동/, "Second landing block must include the developer integration positioning tab.");
assert.doesNotMatch(app, /운영 신뢰/, "Second landing block must not include the operational trust positioning tab.");
assert.match(styles, /\.identity-tabs/, "Second landing block must style the identity tab control.");
assert.match(app, /proofItems/, "Second landing block must define proof row items for each positioning tab.");
assert.match(app, /필터 API 호출/, "Second landing block must phrase endpoint proof as natural Korean.");
assert.match(app, /응답 코드 확인/, "Second landing block must phrase response proof as natural Korean.");
assert.doesNotMatch(app, /visualLabel:\s*"position"|visualLabel:\s*"engine"|visualLabel:\s*"request"|visualLabel:\s*"response"/, "Second landing block visual labels must not expose raw English keys.");
assert.doesNotMatch(app, /visualTitle:\s*"free API"|visualTitle:\s*"Korean trie"|visualTitle:\s*"before save"|visualTitle:\s*"tracked"/, "Second landing block visual titles must be natural Korean sentences.");
assert.doesNotMatch(app, /proofItems:\s*\[[^\]]*"text"|"mode"|"X-API-KEY"|"status\.code"|"trackingId"|"records"/s, "Second landing block proof rows must avoid raw API field names.");
assert.match(styles, /\.identity-proof-row/, "Second landing block must style the proof row.");
assert.match(app, /사용 시나리오/, "Third landing block must introduce a simple usage scenario.");
assert.doesNotMatch(app, /시나리오 영상/, "Scenario panel must not label itself as a video.");
assert.match(styles, /animation:\s*scenario-card-one\s+10s/, "Input card must use the shared 10s scenario loop.");
assert.match(styles, /animation:\s*scenario-card-two\s+10s/, "API card must use the shared 10s scenario loop.");
assert.match(styles, /animation:\s*scenario-card-three\s+10s/, "Result card must use the shared 10s scenario loop.");
assert.doesNotMatch(styles, /animation-delay:\s*1\.4s|animation-delay:\s*2\.8s/, "Scenario card timing must not rely on per-card animation delays.");
assert.match(app, /신청하기/, "Footer CTA must include an apply button.");
assert.match(app, /문서 보기/, "Footer CTA must include a docs button.");
assert.doesNotMatch(app, /title:\s*"2번 블럭"|title:\s*"3번 블럭"|body:\s*"2번 블럭"|body:\s*"3번 블럭"/, "Landing sections must not keep placeholder copy.");

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
