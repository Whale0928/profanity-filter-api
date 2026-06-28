import { lazy, Suspense, useCallback, useEffect, useState } from "react";

import { getCurrentPagePath, type PagePath } from "./constants/pagePath";
import { OPENAPI_DOCUMENT_URL, OVERVIEW_MARKDOWN_PATH, FALLBACK_OVERVIEW_MARKDOWN } from "./docs/constants";
import { preloadDocsDocuments } from "./docs/hooks";
import { HomePage } from "./features/home/HomePage";
import { RegisterPage } from "./features/register/RegisterPage";
import { Navigation } from "./components/Navigation";

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
  const [pagePath, setPagePath] = useState<PagePath>(() => getCurrentPagePath(window.location.pathname));

  const handlePopState = useCallback(() => {
    setPagePath(getCurrentPagePath(window.location.pathname));
  }, []);

  useEffect(() => {
    window.history.scrollRestoration = "manual";
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, [handlePopState]);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, [pagePath]);

  const navigate = useCallback((path: PagePath) => {
    if (window.location.pathname !== path) {
      window.history.pushState({}, "", path);
    }
    setPagePath(path);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, []);

  return (
    <main>
      <Navigation onNavigate={navigate} pagePath={pagePath} />
      {pagePath.startsWith("/docs") ? (
        <Suspense fallback={<DocsFallback />}>
          <DocsPage />
        </Suspense>
      ) : pagePath === "/register" ? (
        <RegisterPage onNavigate={navigate} />
      ) : (
        <HomePage onNavigate={navigate} />
      )}
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
