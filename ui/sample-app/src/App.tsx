import { lazy, Suspense, useCallback, useEffect, useState } from "react";

import { getCurrentPagePath, type PagePath } from "./constants/pagePath";
import { HomePage } from "./features/home/HomePage";
import { Navigation } from "./components/Navigation";

const DocsPage = lazy(() => import("./DocsPage"));

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
      ) : (
        <HomePage onNavigate={navigate} />
      )}
    </main>
  );
}

function DocsFallback() {
  return (
    <section className="docs-loading" aria-label="문서 로딩" role="status">
      문서 로딩 중
    </section>
  );
}
