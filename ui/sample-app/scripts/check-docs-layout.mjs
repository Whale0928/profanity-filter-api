import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
const appRoot = resolve(import.meta.dirname, "..");

const openapi = JSON.parse(readFileSync(resolve(appRoot, "public", "openapi.json"), "utf8"));
const app = readFileSync(resolve(appRoot, "src", "App.tsx"), "utf8");
const docsPage = readFileSync(resolve(appRoot, "src", "DocsPage.tsx"), "utf8");
const guidelines = readFileSync(resolve(appRoot, "DESIGN_GUIDELINES.md"), "utf8");
const styles = readFileSync(resolve(appRoot, "src", "styles.css"), "utf8");
const overview = readFileSync(resolve(appRoot, "public", "overview.md"), "utf8");

assert.match(openapi.openapi, /^3\./, "OpenAPI root must define a 3.x document.");
assert.ok(Object.keys(openapi.paths ?? {}).length > 0, "OpenAPI root must define API paths.");
assert.match(overview, /시작하기/, "Overview markdown must include a getting-started section.");

assert.match(docsPage, /ApiReferenceReact/, "DocsPage must render the OpenAPI body with Scalar.");
assert.match(docsPage, /OPENAPI_DOCUMENT_PATH = "\/openapi\.json"/, "Scalar must read the local OpenAPI JSON file.");
assert.match(docsPage, /OVERVIEW_MARKDOWN_PATH = "\/overview\.md"/, "Docs overview must read the local markdown file.");
assert.match(docsPage, /api-docs-sidebar/, "Docs page must keep the existing fixed sidebar shell.");
assert.match(docsPage, /showSidebar:\s*false/, "Scalar native sidebar must stay hidden inside the fixed docs shell.");
assert.match(docsPage, /buildSections/, "Docs sidebar must be built from OpenAPI tag sections.");
assert.match(docsPage, /parseMarkdownHeadings/, "Docs sidebar must include overview heading children from markdown.");
assert.match(docsPage, /getSectionForHash/, "Docs body must choose overview or an API tag from the docs hash.");
assert.match(docsPage, /content:\s*referenceDocument/, "Docs body must render the selected OpenAPI tag while keeping the docs route.");
assert.match(docsPage, /createTagDocument/, "Docs body must render endpoint DOM for the selected API tag.");
assert.match(docsPage, /section\.operations\.length > 0/, "API groups must expose their endpoint children in the same sidebar.");
assert.match(docsPage, /hideTestRequestButton:\s*true/, "Interactive request execution must stay hidden.");
assert.match(docsPage, /hideClientButton:\s*true/, "Scalar client button must stay hidden.");
assert.match(docsPage, /documentDownloadType:\s*"none"/, "Scalar document download UI must stay hidden.");
assert.doesNotMatch(docsPage, /<strong>문서<\/strong>/, "Docs sidebar must not show a meaningless document group label.");
assert.doesNotMatch(docsPage, />API 레퍼런스</, "Docs sidebar must show OpenAPI groups directly, not a generic API reference item.");
assert.match(docsPage, /href=\{`\/docs#\$\{encodeURIComponent\(createTagAnchor\(section\)\)\}`\}/, "API group links must stay on the docs page and scroll by hash.");
assert.match(docsPage, /encodeURIComponent\(createOperationAnchor\(operation\)\)/, "Expanded endpoints must link to Scalar's encoded operation ids.");
assert.doesNotMatch(docsPage, /<p>OpenAPI<\/p>/, "Selected API group body must not add a custom OpenAPI label above Scalar.");
assert.doesNotMatch(docsPage, /<h1>\{selectedSection\.name\}<\/h1>/, "Selected API group body must let Scalar render the tag heading.");
assert.doesNotMatch(docsPage, /DocsSubnav/, "Docs page must not add a separate top sub-navigation.");
assert.match(docsPage, /id="overview"/, "Overview and OpenAPI must share one docs page with a hash target for overview.");
assert.match(styles, /\.api-docs-sidebar\b/, "Docs CSS must style the existing fixed sidebar shell.");
assert.match(styles, /\.api-docs-sidebar-children\b/, "Docs CSS must style expanded endpoint children.");
assert.match(styles, /--scalar-custom-header-height:\s*0px/, "Scalar must render inside the content area without its own sticky header offset.");
assert.match(styles, /--scalar-sidebar-width:\s*0px/, "Scalar internal sidebar must stay hidden inside the fixed docs shell.");

const docsPageBlock = extractRule(styles, ".api-docs-page");
assert.match(docsPageBlock, /grid-template-columns:\s*280px minmax\(0, 1fr\)/, "Docs page must keep the fixed sidebar column.");

const copyActionsBlock = extractRule(styles, ".api-docs-copy-actions");
assert.match(copyActionsBlock, /position:\s*sticky/, "Docs copy actions must stay in the content flow.");

assert.match(styles, /\.api-docs-overview/, "Docs CSS must style the markdown overview body inside the fixed shell.");

const mobileBlock = extractMedia(styles, "@media (max-width: 767px)");
assert.match(mobileBlock, /\.api-docs-content\s*{[^}]*padding:/s, "Mobile docs layout must keep readable content padding.");
assert.doesNotMatch(mobileBlock, /\.api-docs-sidebar\s*{[^}]*position:\s*fixed/s, "Mobile sidebar must not become a fixed overlay.");

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
