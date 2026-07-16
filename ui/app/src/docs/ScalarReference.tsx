import { ApiReferenceReact, type AnyApiReferenceConfiguration } from "@scalar/api-reference-react";
import "@scalar/api-reference-react/style.css";
import { useMemo } from "react";

import type { Theme } from "../types";
import { createOperationSlug, slugify } from "./navigation";
import type { OpenApiDocument } from "./types";

export default function ScalarReference({ document, theme }: { document: OpenApiDocument; theme: Theme }) {
  const configuration = useMemo<AnyApiReferenceConfiguration>(() => ({
    content: document,
    title: document.info?.title ?? "API 문서",
    slug: "docs",
    theme: "none",
    layout: "modern",
    forceDarkModeState: theme,
    defaultOpenAllTags: true,
    defaultOpenFirstTag: false,
    hideDarkModeToggle: true,
    documentDownloadType: "none",
    hideTestRequestButton: true,
    hideClientButton: true,
    hideModels: true,
    hideSearch: false,
    showSidebar: false,
    showDeveloperTools: "never",
    withDefaultFonts: false,
    agent: { disabled: true },
    mcp: { disabled: true },
    generateTagSlug: ({ name }) => slugify(name ?? ""),
    generateOperationSlug: ({ method, path, operationId, summary }) =>
      createOperationSlug({ method, path, operationId, summary }),
  }), [document, theme]);

  return <div className="api-docs-reference"><ApiReferenceReact configuration={configuration} /></div>;
}
