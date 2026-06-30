import { useEffect, useState } from "react";

import type { MarkdownState, OpenApiDocument, OpenApiDocumentState } from "./types";

type DocumentCacheEntry<T> = {
  promise: Promise<T>;
  status: "pending" | "fulfilled" | "rejected";
  value?: T;
  error?: unknown;
};

type CachedDocumentResult<T> =
  | { status: "fulfilled"; value: T }
  | { status: "rejected"; error: unknown };

export const documentPromiseCache = new Map<string, DocumentCacheEntry<unknown>>();

function createOpenApiCacheKey(url: string) {
  return `openapi:${url}`;
}

function createMarkdownCacheKey(url: string, fallback: string) {
  return `markdown:${url}:${fallback}`;
}

export function getCachedDocumentResult<T>(key: string): CachedDocumentResult<T> | null {
  const cachedDocument = documentPromiseCache.get(key) as DocumentCacheEntry<T> | undefined;
  if (!cachedDocument || cachedDocument.status === "pending") {
    return null;
  }
  if (cachedDocument.status === "fulfilled") {
    return { status: "fulfilled", value: cachedDocument.value as T };
  }
  return { status: "rejected", error: cachedDocument.error };
}

function loadCachedDocument<T>(key: string, loader: () => Promise<T>): Promise<T> {
  const cachedDocument = documentPromiseCache.get(key);
  if (cachedDocument) {
    return cachedDocument.promise as Promise<T>;
  }

  const pendingDocument = {} as DocumentCacheEntry<T>;
  pendingDocument.status = "pending";
  pendingDocument.promise = loader()
    .then((document) => {
      pendingDocument.status = "fulfilled";
      pendingDocument.value = document;
      return document;
    })
    .catch((error: unknown) => {
      pendingDocument.status = "rejected";
      pendingDocument.error = error;
      documentPromiseCache.delete(key);
      throw error;
    });
  documentPromiseCache.set(key, pendingDocument as DocumentCacheEntry<unknown>);
  return pendingDocument.promise;
}

async function loadMarkdownDocument(url: string, fallback: string): Promise<MarkdownState> {
  try {
    const response = await fetch(url);
    if (response.ok) {
      const content = await response.text();
      if (content.trim()) {
        return { content, source: "local", url };
      }
    }
  } catch {
    // API 문서 fetch 실패 시 fallback을 유지해 문서 화면을 계속 표시한다.
  }
  return { content: fallback, source: "fallback", url: "" };
}

async function loadOpenApiDocument(url: string): Promise<OpenApiDocument> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`OpenAPI 문서를 불러오지 못했습니다. status=${response.status}`);
  }
  return response.json() as Promise<OpenApiDocument>;
}

export function preloadDocsDocuments(openApiUrl: string, markdownUrl: string, markdownFallback: string) {
  return Promise.allSettled([
    loadCachedDocument(createOpenApiCacheKey(openApiUrl), () => loadOpenApiDocument(openApiUrl)),
    loadCachedDocument(createMarkdownCacheKey(markdownUrl, markdownFallback), () =>
      loadMarkdownDocument(markdownUrl, markdownFallback),
    ),
  ]);
}

export function useMarkdownDocument(url: string, fallback: string): MarkdownState {
  const [state, setState] = useState<MarkdownState>(() => {
    const cachedDocument = getCachedDocumentResult<MarkdownState>(createMarkdownCacheKey(url, fallback));
    if (cachedDocument?.status === "fulfilled") {
      return cachedDocument.value;
    }
    return {
      content: fallback,
      source: "fallback",
      url: "",
    };
  });

  useEffect(() => {
    let active = true;

    loadCachedDocument(createMarkdownCacheKey(url, fallback), () => loadMarkdownDocument(url, fallback)).then((document) => {
      if (active) {
        setState(document);
      }
    });
    return () => {
      active = false;
    };
  }, [fallback, url]);

  return state;
}

export function useOpenApiDocument(url: string): OpenApiDocumentState {
  const [state, setState] = useState<OpenApiDocumentState>(() => {
    const cachedDocument = getCachedDocumentResult<OpenApiDocument>(createOpenApiCacheKey(url));
    if (cachedDocument?.status === "fulfilled") {
      return { document: cachedDocument.value, error: "" };
    }
    if (cachedDocument?.status === "rejected") {
      return {
        document: null,
        error: cachedDocument.error instanceof Error ? cachedDocument.error.message : "OpenAPI 문서 로딩 실패",
      };
    }
    return {
      document: null,
      error: "",
    };
  });

  useEffect(() => {
    let active = true;

    loadCachedDocument(createOpenApiCacheKey(url), () => loadOpenApiDocument(url))
      .then((document) => {
        if (active) {
          setState({ document, error: "" });
        }
      })
      .catch((loadError: unknown) => {
        if (active) {
          setState({
            document: null,
            error: loadError instanceof Error ? loadError.message : "OpenAPI 문서 로딩 실패",
          });
        }
      });

    return () => {
      active = false;
    };
  }, [url]);

  return state;
}
