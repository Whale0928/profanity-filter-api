import { useCallback, useEffect, useMemo, useState } from "react";

import { getRouteForPathname } from "../app/routes";
import { getCurrentPagePath, type PagePath } from "../constants/pagePath";

export function useAppNavigation() {
  const [pagePath, setPagePath] = useState<PagePath>(() => getCurrentPagePath(window.location.pathname));

  const syncLocation = useCallback(() => {
    setPagePath(getCurrentPagePath(window.location.pathname));
  }, []);

  useEffect(() => {
    window.history.scrollRestoration = "manual";
    window.addEventListener("popstate", syncLocation);
    return () => window.removeEventListener("popstate", syncLocation);
  }, [syncLocation]);

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

  const currentRoute = useMemo(() => getRouteForPathname(pagePath), [pagePath]);

  return {
    currentRoute,
    navigate,
    pagePath,
  };
}
