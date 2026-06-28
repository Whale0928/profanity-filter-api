import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
const appRoot = resolve(import.meta.dirname, "..");

const app = readFileSync(resolve(appRoot, "src", "App.tsx"), "utf8");
const docsPage = readFileSync(resolve(appRoot, "src", "DocsPage.tsx"), "utf8");
const docsConstants = readFileSync(resolve(appRoot, "src", "docs", "constants.ts"), "utf8");
const docsHooks = readFileSync(resolve(appRoot, "src", "docs", "hooks.ts"), "utf8");
const docsUtils = readFileSync(resolve(appRoot, "src", "docs", "utils.tsx"), "utf8");
const homePage = readFileSync(resolve(appRoot, "src", "features", "home", "HomePage.tsx"), "utf8");
const landingContent = readFileSync(resolve(appRoot, "src", "constants", "landingContent.ts"), "utf8");
const guidelines = readFileSync(resolve(appRoot, "DESIGN_GUIDELINES.md"), "utf8");
const styles = readFileSync(resolve(appRoot, "src", "styles.css"), "utf8");
const overview = readFileSync(resolve(appRoot, "public", "overview.md"), "utf8");
const appSources = [app, homePage, landingContent].join("\n");
const docsSources = [docsPage, docsConstants, docsHooks, docsUtils].join("\n");

assert.match(overview, /시작하기/, "Overview markdown must include a getting-started section.");

assert.match(docsPage, /ApiReferenceReact/, "DocsPage must render the OpenAPI body with Scalar.");
assert.match(
  docsConstants,
  /OPENAPI_DOCUMENT_URL = "https:\/\/api\.kr-filter\.com\/openapi\.json"/,
  "Scalar must read the OpenAPI JSON document from the API server.",
);
assert.ok(
  !existsSync(resolve(appRoot, "public", "openapi.json")),
  "The UI bundle must not ship a stale local OpenAPI JSON copy.",
);
assert.match(docsConstants, /OVERVIEW_MARKDOWN_PATH = "https:\/\/api\.kr-filter\.com\/overview\.md"/, "Docs overview must read the API server markdown file.");
assert.match(docsHooks, /documentPromiseCache/, "Remote docs must cache in-flight and completed document loads for the app runtime.");
assert.match(app, /preloadDocsDocuments/, "Docs route loader must start remote document loading before DocsPage renders.");
assert.match(app, /Promise\.all\(\[\s*docsPageModule,\s*docsDocuments,\s*\]\)/, "Docs route loader must wait for the page chunk and both docs with one Suspense loading state.");
assert.match(docsHooks, /preloadDocsDocuments/, "Docs route must expose one preload function that starts every remote docs request together.");
assert.match(docsHooks, /Promise\.allSettled/, "Docs preload must start OpenAPI and overview requests in parallel.");
assert.match(docsHooks, /getCachedDocumentResult/, "Docs hooks must synchronously reuse resolved preload results for initial state.");
assert.doesNotMatch(docsPage, /preloadDocsDocuments\(\s*OPENAPI_DOCUMENT_URL,\s*OVERVIEW_MARKDOWN_PATH,\s*FALLBACK_OVERVIEW_MARKDOWN,\s*\)/, "DocsPage module must not start a second loading phase after the route loader already preloaded docs.");
assert.doesNotMatch(docsSources, /Date\.now\(\)/, "Remote docs must not bypass browser/runtime caching with timestamp query strings.");
assert.match(docsPage, /api-docs-sidebar/, "Docs page must keep the existing fixed sidebar shell.");
assert.match(docsPage, /showSidebar:\s*false/, "Scalar native sidebar must stay hidden inside the fixed docs shell.");
assert.match(docsSources, /buildSections/, "Docs sidebar must be built from OpenAPI tag sections.");
assert.match(docsSources, /parseMarkdownRootHeadings/, "Docs sidebar must include only root overview headings from markdown.");
assert.match(docsUtils, /isMarkdownHorizontalRule/, "Docs markdown parser must detect horizontal rule separator lines.");
assert.match(docsUtils, /<hr/, "Docs markdown parser must render separator lines as horizontal rules.");
assert.match(docsSources, /getSectionForHash/, "Docs body must choose overview or an API tag from the docs hash.");
assert.match(docsPage, /content:\s*document/, "Docs body must render one full OpenAPI document while keeping the docs route.");
assert.doesNotMatch(docsSources, /createTagDocument/, "Docs body must not rebuild a separate OpenAPI document per selected tag.");
assert.match(docsPage, /openSectionSlugs/, "OpenAPI tag groups must keep explicit collapsed or expanded sidebar state.");
assert.match(docsPage, /toggleOpenApiSection/, "OpenAPI tag labels must toggle their endpoint children instead of navigating.");
assert.match(docsPage, /<button[\s\S]*createTagAnchor\(section\)/, "OpenAPI tag labels must render as toggle buttons.");
assert.doesNotMatch(docsPage, /href=\{`\/docs#\$\{encodeURIComponent\(createTagAnchor\(section\)\)\}`\}/, "OpenAPI tag labels must not navigate to a tag hash.");
assert.match(docsPage, /openSectionSlugs\.has\(section\.slug\)/, "Endpoint children must render only when their tag group is open.");
assert.match(docsPage, /section\.operations\.map/, "Open API groups must expose operation links when expanded.");
assert.match(docsPage, /api-docs-root-label[^>]*>\s*Overview\s*</, "Docs sidebar must expose Overview as a root group label.");
assert.match(docsPage, /api-docs-root-label[^>]*>\s*OpenAPI\s*</, "Docs sidebar must expose OpenAPI as a root group label.");
assert.doesNotMatch(docsPage, /<a href="\/docs#overview">\s*<span>Overview<\/span>\s*<\/a>/, "Overview root group must not keep the old fixed Overview text link.");
assert.match(docsPage, /href=\{`\/docs#\$\{encodeURIComponent\(link\.anchor\)\}`\}/, "Overview child links must still navigate by hash.");
assert.match(docsPage, /hideTestRequestButton:\s*true/, "Interactive request execution must stay hidden.");
assert.match(docsPage, /defaultOpenAllTags:\s*true/, "Scalar body must keep all OpenAPI tag sections expanded so operation hash navigation can scroll within one page.");
assert.match(docsPage, /defaultOpenFirstTag:\s*false/, "Scalar body must not rely on only the first tag being open.");
assert.match(docsPage, /import\s+\{\s*ApiReferenceReact/, "DocsPage route chunk must include Scalar so OpenAPI rendering does not show a second loading overlay.");
assert.doesNotMatch(docsPage, /lazy\(\s*\(\)\s*=>\s*import\("@scalar\/api-reference-react"\)/, "Scalar React renderer must not create a second lazy loading phase inside DocsPage.");
assert.match(docsPage, /hideClientButton:\s*true/, "Scalar client button must stay hidden.");
assert.match(docsPage, /documentDownloadType:\s*"none"/, "Scalar document download UI must stay hidden.");
assert.doesNotMatch(docsPage, /<strong>문서<\/strong>/, "Docs sidebar must not show a meaningless document group label.");
assert.doesNotMatch(docsPage, />API 레퍼런스</, "Docs sidebar must show OpenAPI groups directly, not a generic API reference item.");
assert.match(docsPage, /href=\{`\/docs#\$\{encodeURIComponent\(createOperationAnchor\(operation\)\)\}`\}/, "API operation links must stay on the docs page and scroll by hash.");
assert.match(docsPage, /encodeURIComponent\(createOperationAnchor\(operation\)\)/, "Expanded endpoints must link to Scalar's encoded operation ids.");
assert.doesNotMatch(docsPage, /<p>OpenAPI<\/p>/, "Selected API group body must not add a custom OpenAPI label above Scalar.");
assert.doesNotMatch(docsPage, /<h1>\{selectedSection\.name\}<\/h1>/, "Selected API group body must let Scalar render the tag heading.");
assert.doesNotMatch(docsPage, /DocsSubnav/, "Docs page must not add a separate top sub-navigation.");
assert.match(docsPage, /id="overview"/, "Overview and OpenAPI must share one docs page with a hash target for overview.");
assert.match(styles, /\.api-docs-sidebar\b/, "Docs CSS must style the existing fixed sidebar shell.");
assert.match(styles, /\.api-docs-root-group\b/, "Docs CSS must visually separate Overview and OpenAPI root groups.");
assert.match(styles, /\.api-docs-root-label\b/, "Docs CSS must style docs sidebar root group labels.");
assert.match(styles, /\.docs-loading-bar\b/, "Docs loading state must show a centered loading bar instead of text-only loading.");
assert.doesNotMatch(docsPage, /<Suspense fallback=\{<DocsLoadingFrame \/>}>/, "Scalar body must not show a second docs loading overlay after the route loader finishes.");
assert.match(app, /<DocsLoadingOverlay \/>/, "App docs route loading must use the shared viewport-centered loading overlay.");
assert.match(docsPage, /<DocsLoadingFrame \/>/, "Docs internal loading must use the same viewport-centered loading frame.");
assert.match(extractRule(styles, ".docs-loading-overlay"), /position:\s*fixed/, "Docs loading overlay must stay fixed to the browser viewport.");
assert.match(extractRule(styles, ".docs-loading-overlay"), /inset:\s*0/, "Docs loading overlay must cover the browser viewport.");
assert.match(extractRule(styles, ".docs-loading-overlay"), /place-items:\s*center/, "Docs loading overlay must center the loading bar consistently.");
assert.match(styles, /\.docs-markdown-article hr\b/, "Docs markdown horizontal rules must be styled as separators.");
assert.match(styles, /\.api-docs-sidebar-children\b/, "Docs CSS must style expanded endpoint children.");
assert.match(styles, /--scalar-custom-header-height:\s*0px/, "Scalar must render inside the content area without its own sticky header offset.");
assert.match(styles, /--scalar-sidebar-width:\s*0px/, "Scalar internal sidebar must stay hidden inside the fixed docs shell.");

const docsPageBlock = extractRule(styles, ".api-docs-page");
assert.match(docsPageBlock, /grid-template-columns:\s*280px minmax\(0, 1fr\)/, "Docs page must keep the fixed sidebar column.");

const copyActionsBlock = extractRule(styles, ".api-docs-copy-actions");
assert.match(copyActionsBlock, /position:\s*sticky/, "Docs copy actions must stay in the content flow.");

assert.match(styles, /\.api-docs-overview/, "Docs CSS must style the markdown overview body inside the fixed shell.");
assert.match(styles, /\.docs-markdown-article/, "Docs markdown CSS must avoid Scalar's markdown-article class name.");
assert.doesNotMatch(styles, /\.markdown-article\b/, "Docs markdown CSS must not collide with Scalar's markdown-article class.");
assert.match(extractRule(styles, ".api-docs-content"), /max-width:\s*100%/, "Docs content must not exceed the available width.");
assert.match(extractRule(styles, ".api-docs-main"), /max-width:\s*100%/, "Docs main rail must not exceed the available width.");
assert.match(extractRule(styles, ".api-docs-overview"), /min-width:\s*0/, "Docs overview must be allowed to shrink.");
assert.match(extractRule(styles, ".docs-markdown-article"), /max-width:\s*100%/, "Docs markdown must not exceed the content width.");
assert.match(extractRule(styles, ".markdown-table-wrap"), /overflow-x:\s*auto/, "Wide markdown tables must scroll inside their own container.");

const mobileBlock = extractMedia(styles, "@media (max-width: 767px)");
assert.match(mobileBlock, /\.api-docs-content\s*{[^}]*padding:/s, "Mobile docs layout must keep readable content padding.");
assert.doesNotMatch(mobileBlock, /\.api-docs-content\s*{[^}]*padding:\s*0\s*;/s, "Mobile docs content must keep horizontal padding instead of touching viewport edges.");
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
assert.match(appSources, /한국어 문장을 API로 필터링합니다/, "Second landing block must explain what the project does.");
assert.match(appSources, /role="tablist"/, "Second landing block must expose type options as tabs.");
assert.match(appSources, /무료\/실용/, "Second landing block must include the free practical positioning tab.");
assert.match(appSources, /한국어 중심/, "Second landing block must include the Korean-focused positioning tab.");
assert.match(appSources, /개발자 연동/, "Second landing block must include the developer integration positioning tab.");
assert.doesNotMatch(appSources, /운영 신뢰/, "Second landing block must not include the operational trust positioning tab.");
assert.match(styles, /\.identity-tabs/, "Second landing block must style the identity tab control.");
assert.match(appSources, /proofItems/, "Second landing block must define proof row items for each positioning tab.");
assert.match(appSources, /필터 API 호출/, "Second landing block must phrase endpoint proof as natural Korean.");
assert.match(appSources, /응답 코드 확인/, "Second landing block must phrase response proof as natural Korean.");
assert.doesNotMatch(appSources, /visualLabel:\s*"position"|visualLabel:\s*"engine"|visualLabel:\s*"request"|visualLabel:\s*"response"/, "Second landing block visual labels must not expose raw English keys.");
assert.doesNotMatch(appSources, /visualTitle:\s*"free API"|visualTitle:\s*"Korean trie"|visualTitle:\s*"before save"|visualTitle:\s*"tracked"/, "Second landing block visual titles must be natural Korean sentences.");
assert.doesNotMatch(appSources, /proofItems:\s*\[[^\]]*"text"|"mode"|"X-API-KEY"|"status\.code"|"trackingId"|"records"/s, "Second landing block proof rows must avoid raw API field names.");
assert.match(styles, /\.identity-proof-row/, "Second landing block must style the proof row.");
assert.match(appSources, /사용 시나리오/, "Third landing block must introduce a simple usage scenario.");
assert.doesNotMatch(appSources, /시나리오 영상/, "Scenario panel must not label itself as a video.");
assert.match(styles, /animation:\s*scenario-card-one\s+10s/, "Input card must use the shared 10s scenario loop.");
assert.match(styles, /animation:\s*scenario-card-two\s+10s/, "API card must use the shared 10s scenario loop.");
assert.match(styles, /animation:\s*scenario-card-three\s+10s/, "Result card must use the shared 10s scenario loop.");
assert.doesNotMatch(styles, /animation-delay:\s*1\.4s|animation-delay:\s*2\.8s/, "Scenario card timing must not rely on per-card animation delays.");
assert.match(appSources, /신청하기/, "Footer CTA must include an apply button.");
assert.match(appSources, /문서 보기/, "Footer CTA must include a docs button.");
assert.doesNotMatch(appSources, /title:\s*"2번 블럭"|title:\s*"3번 블럭"|body:\s*"2번 블럭"|body:\s*"3번 블럭"/, "Landing sections must not keep placeholder copy.");

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
