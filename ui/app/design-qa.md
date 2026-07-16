# Product Design QA

- Source visual truth: `design-qa-evidence/source-credentials-dark.png`
- Final implementation screenshot: `design-audit/03-credentials-after.png`
- Full-view comparison: `design-qa-evidence/desktop-comparison-v4.png`
- Focused comparison: `design-qa-evidence/focused-comparison-v4.png`
- Additional states: `design-audit/04-docs-markdown-after.png`, `design-audit/05-json-code-after.png`, `design-audit/06-scalar-after.png`, `design-audit/07-credentials-mobile-after.png`, `design-audit/08-docs-sidebar-tree.png`
- Primary viewport: `1440 x 1024`
- Responsive viewport: `390 x 844`
- State: signed in, credentials route, dark theme

## Full-view comparison evidence

The final implementation preserves the selected visual structure: one global header, one signed-in navigation row, a two-column credential comparison, restrained emerald accents, one center divider, aligned actions and a bottom security note. The revised content intentionally replaces single-header examples with complete request examples and lowers the typography scale per user feedback.

## Focused comparison evidence

The focused OAuth comparison verifies the single-line title, icon scale, recommended label, request example, three flow steps and primary action. The implementation uses Noto Sans KR Variable instead of the generated mock's approximate sans rendering and intentionally reduces the optical scale.

## Required fidelity surfaces

- Fonts and typography: passed. Noto Sans KR Variable remains consistent across the product. Page titles now top out at `42px`, the signed-in greeting at `44px`, navigation at `14px`, and credential titles at `28px`; the OAuth2 title remains one line at desktop and `20px` mobile.
- Spacing and layout rhythm: passed. At `1440px`, the main rail is `1180px` wide with `130px` gutters. Credential methods use matched row tracks; both measure `508px` high and both action controls begin at the same `y=790` coordinate.
- Colors and visual tokens: passed. Charcoal, graphite, warm foreground and emerald tokens map consistently between dark and light themes. Active, focus and recommended states are not color-only.
- Image quality and asset fidelity: passed. The source contains no product imagery or custom raster assets. All visible interface icons use one Phosphor icon family; no custom SVG, CSS art, emoji or placeholder imagery is used.
- Copy and content: passed. The page never displays a real API Key, Client Secret or access token. Both methods explain the complete request shape with environment-variable placeholders, and OAuth2 is described as a token-request-plus-Bearer flow rather than a single JWT header.
- Responsiveness: passed. At `390 x 844`, the credential page has no horizontal overflow, the OAuth2 title remains one line, and stacked methods retain readable request examples.
- Accessibility and behavior: passed. Semantic navigation, current-page state, focus-visible styling, reduced-motion support, descriptive labels and practical tap targets are present. The theme control is visually icon-only while retaining an explicit accessible name.

## Interaction verification

- Public and signed-in navigation: passed.
- API Key creation action opens the prototype-only dialog: passed.
- Dialog closes without creating or transmitting a credential: passed.
- API document navigation and section selection: passed. Both remote documents are prefetched once and cached. Overview renders `2` fenced code blocks and `5` Markdown tables; the OpenAPI reference is rendered by Scalar from the live `v1` document.
- Account, local sign-out, login and signed-in start flow: passed.
- Light/dark theme toggle and browser persistence: passed.
- Mobile menu exposes public navigation and theme toggle: passed.
- Browser console errors and warnings: none.

## Comparison history

### Iteration 1

- Finding: P2 spacing drift. The implementation content began about 40px lower than the source and the top navigation was more inset.
- Fix: reduced credentials page top padding and grid gap; aligned global and signed-in navigation padding.
- Post-fix evidence: `desktop-comparison-v2.png`.

### Iteration 2

- Finding: P2 typography density. The focused comparison showed explanatory copy, list rows and actions reading one optical step smaller than the source.
- Fix: increased method description, label, code, list, action and security-note typography without changing the selected layout.
- Post-fix evidence: `desktop-comparison-v3.png`, `focused-comparison-v3.png`.

### Iteration 3

- Finding: P2 page density. Content extended too close to the viewport edges, the signed-in start heading was oversized, and the theme control repeated its meaning in text.
- Fix: introduced one shared ratio-aware content rail, reduced the start heading to `52px` desktop and `36px` mobile, and kept only the sun/moon icon plus switch in the visible theme control.
- Finding: P2 documentation fidelity. The documentation screen used static prototype content instead of the two public documentation endpoints.
- Fix: fetched and rendered `/overview.md` and `/openapi.json` directly, with independent loading and failure states so one source can remain usable if the other fails.
- Post-fix evidence: `start-dark-letterbox.png`, `start-mobile-letterbox.png`, `docs-openapi-live.png`, `credentials-dark-desktop-v4.png`.

### Iteration 4

- Finding: P1 documentation rendering. Fenced JSON appeared as literal backticks and loose paragraphs, and tables were discarded.
- Fix: replaced the line-by-line renderer with block-aware Markdown parsing for fenced code, ordered and unordered lists, tables, headings and paragraphs.
- Finding: P2 documentation experience. `/docs` waited for both requests after route entry and the custom OpenAPI rows omitted reference detail.
- Fix: started both requests at application load, reused a shared promise cache, resolved each document independently, and restored the prior Scalar-based reference.
- Finding: P2 credentials hierarchy. Page and navigation typography was oversized, OAuth2 wrapped awkwardly, and corresponding content rows did not share a baseline.
- Fix: lowered the product typography scale, kept the OAuth2 title on one line, changed both methods to complete request examples, and aligned both columns with shared grid tracks.
- Post-fix evidence: `design-audit/03-credentials-after.png`, `design-audit/05-json-code-after.png`, `design-audit/06-scalar-after.png`, `desktop-comparison-v4.png`, `focused-comparison-v4.png`.

### Iteration 5

- Finding: P2 documentation navigation. The fixed three-button sidebar did not reflect Markdown H1 headings or expose the OpenAPI tag and summary hierarchy.
- Fix: generated Markdown anchors from live `#` headings, added a divider, rendered all six OpenAPI tag groups collapsed by default, and connected summary links to matching Scalar operation IDs through a shared slug policy.
- Post-fix evidence: `design-audit/08-docs-sidebar-tree.png`; browser checks confirmed `Error Model` at `top=24px` and `docs/tag/sync/get-api-v1-sync` at `top=24px` after navigation.

## Findings

No actionable P0, P1 or P2 findings remain.

## Follow-up polish

- P3: The generated source uses slightly softer antialiasing than the browser-rendered local font. This expected rendering difference does not change hierarchy or usability.

final result: passed
