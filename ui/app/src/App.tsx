import { lazy, Suspense } from "react";

import { OPENAPI_DOCUMENT_URL, OVERVIEW_MARKDOWN_PATH, FALLBACK_OVERVIEW_MARKDOWN } from "./docs/constants";
import { preloadDocsDocuments } from "./docs/hooks";
import { Navigation } from "./components/Navigation";
import { useAppNavigation } from "./hooks/useAppNavigation";

function loadDocsPage() {
  const docsPageModule = import("./DocsPage");
  const docsDocuments = preloadDocsDocuments(
    OPENAPI_DOCUMENT_URL,
    OVERVIEW_MARKDOWN_PATH,
    FALLBACK_OVERVIEW_MARKDOWN,
  );

  return Promise.all([
    docsPageModule,
    docsDocuments,
  ]).then(([module]) => module);
}

const DocsPage = lazy(loadDocsPage);

export default function App() {
  const { currentRoute, navigate, pagePath } = useAppNavigation();
  const docsElement = (
    <Suspense fallback={<DocsFallback />}>
      <DocsPage />
    </Suspense>
  );

  return (
    <main>
      <Navigation onNavigate={navigate} pagePath={pagePath} />
      {currentRoute.render({ docsElement, navigate })}
    </main>
  );
}

function DocsFallback() {
  return <DocsLoadingOverlay />;
}

function DocsLoadingOverlay() {
  return (
    <div className="docs-loading-overlay" aria-label="문서 로딩" role="status">
      <div className="docs-loading-bar">
        <span />
      </div>
    </div>
  );
}
